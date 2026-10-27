"""
Reference implementation (Python) of the day algorithm for «اليوم المتكامل».
It is the executable specification that the Kotlin implementation must match.

- night_plan(): sleep budget for the night after Isha (bedtime, wake, qiyam/witr, nap, shortfall)
- resolve_day(): fits the day template into the segments between prayer anchors
- check_invariants(): the properties every resolved day must satisfy

Run:  python resolver_reference.py   (needs: pip install adhanpy)
All times are minutes from local midnight; values >= 1440 belong to the next day.
"""
from __future__ import annotations
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

NEED_SLEEP = 420            # 7 h (AASM/SRS adult recommendation)
ISHA_TO_BED_MIN = 30        # Isha prayer + witr/adhkar before sleeping
WAKE_BEFORE_FAJR_QIYAM = 20
WAKE_BEFORE_FAJR_PLAIN = 10
MIN_NIGHT_FOR_QIYAM = 360   # below 6 h of night sleep, no waking before Fajr: witr before sleep
SHORT_NAP, LONG_NAP = 20, 90  # avoid 30–90 min naps (sleep inertia)


# ---------------------------------------------------------------- night plan
@dataclass
class NightPlan:
    bedtime: int
    wake: int
    qiyam: bool              # waking before Fajr for night prayer + witr
    witr_before_sleep: bool
    night_sleep: int
    nap: int
    total: int
    shortfall: int           # > 0 means the 7 h target cannot be met; UI must say so honestly


def night_plan(isha: int, next_fajr: int, user_bedtime: int = 21 * 60, qiyam_enabled: bool = True) -> NightPlan:
    """isha in today's minutes; next_fajr in minutes from today's midnight (so ~1440+)."""
    bedtime = max(user_bedtime, isha + ISHA_TO_BED_MIN)
    qiyam = False
    if qiyam_enabled and (next_fajr - WAKE_BEFORE_FAJR_QIYAM) - bedtime >= MIN_NIGHT_FOR_QIYAM:
        qiyam = True
    wake = next_fajr - (WAKE_BEFORE_FAJR_QIYAM if qiyam else WAKE_BEFORE_FAJR_PLAIN)
    night = max(0, wake - bedtime)
    deficit = NEED_SLEEP - night
    nap = SHORT_NAP if deficit <= 30 else LONG_NAP
    total = night + nap
    return NightPlan(bedtime, wake, qiyam, not qiyam, night, nap, total, max(0, NEED_SLEEP - total))


# ---------------------------------------------------------------- template
@dataclass
class Block:
    key: str
    title: str
    role: str
    pref: int                # preferred minutes (ignored for flex)
    min: int                 # never shrink below this
    priority: int            # 1 = most important; higher numbers shrink/drop first
    droppable: bool = True
    flex: bool = False       # absorbs the remaining time of its segment (exactly one per segment, or none)
    deep: bool = False
    is_prayer: bool = False


@dataclass
class Resolved:
    key: str
    title: str
    role: str
    start: int
    end: int
    deep: bool
    is_prayer: bool
    shrunk_from: int | None = None


# Segments: (start_anchor, end_anchor, blocks). Anchors: QIYAM_WAKE, FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA, BED
def workday_template(p_nap: int) -> list[tuple[str, str, list[Block]]]:
    return [
        ("FAJR", "SUNRISE", [
            Block("fajr", "صلاة الفجر جماعة", "WORSHIP", 25, 15, 1, droppable=False, is_prayer=True),
            Block("ghadwa", "ورد الغدوة: أذكار وقرآن، بلا تلفون", "WORSHIP", 0, 15, 2, droppable=False, flex=True),
        ]),
        ("SUNRISE", "DHUHR", [
            Block("light", "ضوء الشمس ومشي وفطور", "BODY", 25, 10, 3),
            Block("deep1", "تركيز: أصعب مهمة", "DEV", 90, 45, 2, droppable=False, deep=True),
            Block("duha", "استراحة وركعتا الضحى", "WORSHIP", 20, 10, 4),
            Block("deep2", "تركيز: الكتلة الثانية", "DEV", 90, 45, 5, deep=True),
            Block("msgs1", "نافذة الرسائل", "BUSINESS", 20, 10, 4),
            Block("work1", "العمل: اتصالات ومتابعة", "BUSINESS", 0, 0, 6, flex=True),
        ]),
        ("DHUHR", "ASR", [
            Block("dhuhr", "صلاة الظهر وغداء خفيف", "WORSHIP", 40, 25, 1, droppable=False, is_prayer=True),
            Block("nap", f"قيلولة {p_nap} دقيقة", "BODY", p_nap, SHORT_NAP, 2, droppable=False),
            Block("work2", "العمل: ميدان واجتماعات", "BUSINESS", 0, 0, 6, flex=True),
        ]),
        ("ASR", "MAGHRIB", [
            Block("asr", "صلاة العصر", "WORSHIP", 12, 10, 1, droppable=False, is_prayer=True),
            Block("train", "تدريب أو تعلم (حسب اليوم)", "BODY", 60, 30, 4),
            Block("close", "الرسائل وإغلاق يوم العمل", "BUSINESS", 20, 10, 5),
            Block("child", "وقت الابن وأذكار المساء", "CHILD", 0, 30, 2, droppable=False, flex=True),
        ]),
        ("MAGHRIB", "ISHA", [
            Block("maghrib", "صلاة المغرب", "WORSHIP", 15, 10, 1, droppable=False, is_prayer=True),
            Block("family", "الأسرة: عشاء وحديث مع الزوجة", "SPOUSE", 0, 30, 2, droppable=False, flex=True),
        ]),
        ("ISHA", "BED", [
            Block("isha", "صلاة العشاء", "WORSHIP", 15, 10, 1, droppable=False, is_prayer=True),
            Block("self", "وقت لك: قراءة أو تعلم خفيف", "SELF", 0, 0, 6, flex=True),
            Block("prep", "الاستعداد للنوم: محاسبة وترتيب الغد", "SELF", 30, 15, 3),
            Block("witr", "الوتر وأذكار النوم", "WORSHIP", 15, 10, 1, droppable=False),
        ]),
    ]


def friday_template(p_nap: int) -> list[tuple[str, str, list[Block]]]:
    t = workday_template(p_nap)
    t[1] = ("SUNRISE", "DHUHR", [
        Block("light", "ضوء الشمس ومشي وفطور مع الأهل", "BODY", 40, 15, 3),
        Block("self_am", "وقت لك أو للأسرة", "SELF", 0, 0, 6, flex=True),
        Block("jumua_prep", "التبكير للجمعة: غُسل وسورة الكهف", "WORSHIP", 60, 30, 1, droppable=False),
    ])
    t[2] = ("DHUHR", "ASR", [
        Block("jumua", "الجمعة: الخطبة والصلاة", "WORSHIP", 50, 40, 1, droppable=False, is_prayer=True),
        Block("lunch", "غداء مع الأسرة", "SPOUSE", 40, 20, 3),
        Block("nap", f"قيلولة {p_nap} دقيقة", "BODY", p_nap, SHORT_NAP, 2, droppable=False),
        Block("visit", "صلة الرحم ونزهة الأبناء", "CHILD", 0, 0, 6, flex=True),
    ])
    t[3] = ("ASR", "MAGHRIB", [
        Block("asr", "صلاة العصر", "WORSHIP", 12, 10, 1, droppable=False, is_prayer=True),
        Block("free", "فراغ مباح مع الأهل", "SELF", 0, 0, 6, flex=True),
        Block("dua", "بلا تلفون: دعاء آخر ساعة من الجمعة", "WORSHIP", 60, 30, 2, droppable=False),
    ])
    return t


@dataclass
class Segment:
    start: int
    end: int
    blocks: list[Resolved] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


def fit_segment(start: int, end: int, blocks: list[Block]) -> Segment:
    seg = Segment(start, end)
    avail = end - start
    dur = {b.key: (b.min if b.flex else b.pref) for b in blocks}
    live = list(blocks)

    def total() -> int:
        return sum(dur[b.key] for b in live)

    def shrink(pool):
        for b in sorted(pool, key=lambda x: -x.priority):
            over = total() - avail
            if over <= 0:
                return
            cut = min(over, dur[b.key] - b.min)
            if cut > 0:
                dur[b.key] -= cut
                seg.notes.append(f"shrunk:{b.key}")

    # 1) shrink optional blocks toward their minimum, lowest priority first
    shrink([b for b in live if b.droppable and not b.flex])
    # 2) drop optional blocks, lowest priority first
    for b in sorted([b for b in live if b.droppable and not b.flex], key=lambda x: -x.priority):
        if total() - avail <= 0:
            break
        live.remove(b)
        seg.notes.append(f"dropped:{b.key}")
    # 3) only then shrink required blocks (prayers, witr, nap, family/child minimums) toward their minimum
    shrink([b for b in live if not b.droppable and not b.flex])
    # 3) give the slack to the flex block
    slack = avail - total()
    if slack > 0:
        flex = next((b for b in live if b.flex), None)
        if flex:
            dur[flex.key] += slack
    if total() > avail:
        seg.notes.append("overflow")  # must never happen for supported cities; checked by invariants
    t = start
    for b in live:
        d = dur[b.key]
        if d <= 0:
            continue
        seg.blocks.append(Resolved(b.key, b.title, b.role, t, t + d, b.deep, b.is_prayer,
                                   shrunk_from=(b.pref if (not b.flex and d < b.pref) else None)))
        t += d
    return seg


@dataclass
class Day:
    date: date
    times: dict
    plan: NightPlan
    prev_plan: NightPlan
    blocks: list[Resolved]
    notes: list[str]


def resolve_day(d: date, times: dict, next_fajr: int, prev_isha: int, today_fajr: int,
                user_bedtime: int = 21 * 60, qiyam_enabled: bool = True, weekday: int | None = None) -> Day:
    plan = night_plan(times["isha"], next_fajr, user_bedtime, qiyam_enabled)
    prev_plan = night_plan(prev_isha, today_fajr + 1440, user_bedtime, qiyam_enabled)
    wd = d.weekday() if weekday is None else weekday   # Monday=0 ... Friday=4
    tmpl = friday_template(plan.nap) if wd == 4 else workday_template(plan.nap)
    anchors = dict(FAJR=times["fajr"], SUNRISE=times["sunrise"], DHUHR=times["dhuhr"], ASR=times["asr"],
                   MAGHRIB=times["maghrib"], ISHA=times["isha"], BED=plan.bedtime)
    blocks, notes = [], []
    if prev_plan.qiyam:
        blocks.append(Resolved("qiyam", "السَّحَر: ركعتان والوتر", "WORSHIP", prev_plan.wake - 1440, times["fajr"], False, False))
    for a, b, bl in tmpl:
        seg = fit_segment(anchors[a], anchors[b], bl)
        blocks += seg.blocks
        notes += seg.notes
    return Day(d, times, plan, prev_plan, blocks, notes)


# ---------------------------------------------------------------- invariants
def check_invariants(day: Day) -> list[str]:
    e = []
    bl = day.blocks
    for x, y in zip(bl, bl[1:]):
        if x.end > y.start:
            e.append(f"overlap {x.key}->{y.key}")
    for b in bl:
        if b.end <= b.start:
            e.append(f"non-positive {b.key}")
    prayer_times = [day.times[k] for k in ("fajr", "dhuhr", "asr", "maghrib", "isha")]
    for b in bl:
        for p in prayer_times:
            if b.start < p < b.end:
                e.append(f"crosses prayer {b.key}")
    for k, anchor in (("fajr", "fajr"), ("asr", "asr"), ("maghrib", "maghrib"), ("isha", "isha")):
        blk = next((b for b in bl if b.key == k), None)
        if not blk or blk.start != day.times[anchor]:
            e.append(f"prayer block {k} not at its time")
    dh = next((b for b in bl if b.key in ("dhuhr", "jumua")), None)
    if not dh or dh.start != day.times["dhuhr"]:
        e.append("dhuhr/jumua not at its time")
    for key in ("child", "family"):
        blk = next((b for b in bl if b.key == key), None)
        if blk and blk.end - blk.start < 30:
            e.append(f"{key} < 30 min")
    if day.date.weekday() != 4 and not any(b.deep and b.end - b.start >= 45 for b in bl):
        e.append("no deep block >= 45")
    p = day.plan
    if p.bedtime < day.times["isha"] + ISHA_TO_BED_MIN:
        e.append("bed before isha+30")
    if p.total < NEED_SLEEP and p.shortfall != NEED_SLEEP - p.total:
        e.append("sleep shortfall not flagged")
    for b in bl:
        if b.is_prayer and b.end - b.start < 10:
            e.append(f"prayer block {b.key} < 10 min")
    if any(n.startswith("shrunk:") for n in day.notes if n.split(":")[1] in ("fajr", "dhuhr", "jumua", "asr", "maghrib", "isha", "witr")) \
            and not any(n.startswith("dropped:") for n in day.notes):
        e.append("required block shrunk while optional blocks were still available")
    if "overflow" in day.notes:
        e.append("segment overflow")
    return e


# ---------------------------------------------------------------- simulation
def prayer_times(lat, lon, tz, d, madhab="SHAFI", high_lat=None, method="EGYPTIAN"):
    from adhanpy.PrayerTimes import PrayerTimes
    from adhanpy.calculation.CalculationMethod import CalculationMethod as CM
    from adhanpy.calculation.CalculationParameters import CalculationParameters
    from adhanpy.calculation.Madhab import Madhab
    params = CalculationParameters(method=getattr(CM, method))
    params.madhab = Madhab.HANAFI if madhab == "HANAFI" else Madhab.SHAFI
    if high_lat is not None:
        from adhanpy.calculation.HighLatitudeRule import HighLatitudeRule
        params.high_latitude_rule = getattr(HighLatitudeRule, high_lat)
    pt = PrayerTimes((lat, lon), datetime(d.year, d.month, d.day), calculation_parameters=params, time_zone=ZoneInfo(tz))
    midnight = datetime(d.year, d.month, d.day, tzinfo=ZoneInfo(tz))
    # minutes from this date's local midnight; may exceed 1440 (e.g. Isha after midnight in London summer)
    f = lambda x: round((x.astimezone(ZoneInfo(tz)) - midnight).total_seconds() / 60)
    return dict(fajr=f(pt.fajr), sunrise=f(pt.sunrise), dhuhr=f(pt.dhuhr), asr=f(pt.asr), maghrib=f(pt.maghrib), isha=f(pt.isha))


CASES = {
    "sulaymaniyah-shafi": (35.5613, 45.4375, "Asia/Baghdad", "SHAFI", "EGYPTIAN", None),
    "sulaymaniyah-hanafi": (35.5613, 45.4375, "Asia/Baghdad", "HANAFI", "EGYPTIAN", None),
    "london-shafi": (51.5074, -0.1278, "Europe/London", "SHAFI", "MUSLIM_WORLD_LEAGUE", "SEVENTH_OF_THE_NIGHT"),
}


def simulate(year=2026):
    import csv, json, os
    os.makedirs("out", exist_ok=True)
    summary = {}
    rows = []
    samples = {}
    for name, (lat, lon, tz, mad, meth, hl) in CASES.items():
        days = [date(year, 1, 1) + timedelta(i) for i in range(365)]
        T = {d: prayer_times(lat, lon, tz, d, mad, hl, meth) for d in [days[0] - timedelta(1)] + days + [days[-1] + timedelta(1)]}
        fails, shortfall_days, shrinks = [], 0, {}
        for d in days:
            t, nx, pv = T[d], T[d + timedelta(1)], T[d - timedelta(1)]
            day = resolve_day(d, t, nx["fajr"] + 1440, pv["isha"], t["fajr"])
            errs = check_invariants(day)
            if errs:
                fails.append((d.isoformat(), errs))
            if day.plan.shortfall > 0:
                shortfall_days += 1
            for n in day.notes:
                shrinks[n] = shrinks.get(n, 0) + 1
            p = day.plan
            hhmm = lambda v: f"{(v % 1440) // 60:02d}:{v % 60:02d}" + ("+1" if v >= 1440 else "")
            rows.append(dict(case=name, date=d.isoformat(), **{k: hhmm(v) for k, v in t.items()},
                             bedtime=f"{p.bedtime // 60 % 24:02d}:{p.bedtime % 60:02d}",
                             wake=f"{p.wake // 60 % 24:02d}:{p.wake % 60:02d}", qiyam=int(p.qiyam),
                             night_sleep_min=p.night_sleep, nap_min=p.nap, total_sleep_min=p.total, shortfall_min=p.shortfall))
            if (name, d.isoformat()) in SAMPLE_DAYS:
                samples[f"{name}:{d.isoformat()}"] = dict(
                    times={k: f"{(v % 1440) // 60:02d}:{v % 60:02d}" + ("+1" if v >= 1440 else "") for k, v in t.items()},
                    night_plan=p.__dict__, notes=day.notes,
                    blocks=[dict(key=b.key, title=b.title, role=b.role,
                                 start=f"{(b.start % 1440) // 60:02d}:{b.start % 60:02d}",
                                 end=f"{(b.end % 1440) // 60:02d}:{b.end % 60:02d}", minutes=b.end - b.start) for b in day.blocks])
        summary[name] = dict(failures=len(fails), first_failures=fails[:3], shortfall_days=shortfall_days,
                             adjustments=dict(sorted(shrinks.items(), key=lambda kv: -kv[1])))
    with open("out/reference-2026.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    with open("out/sample-days.json", "w", encoding="utf-8") as f:
        json.dump(samples, f, ensure_ascii=False, indent=1)
    return summary


SAMPLE_DAYS = {
    ("sulaymaniyah-shafi", "2026-10-15"), ("sulaymaniyah-shafi", "2026-06-15"), ("sulaymaniyah-shafi", "2026-12-21"),
    ("sulaymaniyah-shafi", "2026-05-03"), ("sulaymaniyah-shafi", "2026-10-16"),  # 16 Oct 2026 is a Friday
    ("sulaymaniyah-hanafi", "2026-01-10"), ("london-shafi", "2026-12-21"), ("london-shafi", "2026-06-21"),
}

if __name__ == "__main__":
    import json
    print(json.dumps(simulate(), ensure_ascii=False, indent=1))
