/* Yawm engine: a faithful JS port of docs/reference/resolver_reference.py (night plan + segment fitting),
   plus a NOAA solar-position model for the sundial. Prayer times come from the reference CSV (adhanpy). */
const NEED_SLEEP = 420, ISHA_TO_BED_MIN = 30, WAKE_BEFORE_FAJR_QIYAM = 20, WAKE_BEFORE_FAJR_PLAIN = 10,
  MIN_NIGHT_FOR_QIYAM = 360, SHORT_NAP = 20, LONG_NAP = 90;

const CITIES = {
  sul: { name: 'السليمانية', lat: 35.5613, lon: 45.4375, cases: { SHAFI: 'sulaymaniyah-shafi', HANAFI: 'sulaymaniyah-hanafi' },
    method: 'الهيئة المصرية العامة للمساحة' },
  lon: { name: 'لندن', lat: 51.5074, lon: -0.1278, cases: { SHAFI: 'london-shafi' },
    method: 'رابطة العالم الإسلامي، مع قاعدة سُبع الليل' },
};

function lastSundayUTC(year, month) { // month 0-based; returns Date.UTC of last Sunday 01:00 UTC
  const d = new Date(Date.UTC(year, month + 1, 0));
  d.setUTCDate(d.getUTCDate() - d.getUTCDay());
  return Date.UTC(year, month, d.getUTCDate(), 1);
}
function utcOffset(cityKey, doy) {
  if (cityKey === 'sul') return 180;
  const noon = Date.UTC(2026, 0, 1 + doy, 12);
  return noon >= lastSundayUTC(2026, 2) && noon < lastSundayUTC(2026, 9) ? 60 : 0;
}
const clampDoy = (d) => Math.max(0, Math.min(364, d));
function times(cityKey, madhab, doy) {
  const c = CITIES[cityKey];
  const key = c.cases[madhab] || c.cases.SHAFI;
  const r = YAWM_TIMES[key][clampDoy(doy)];
  return { fajr: r[0], sunrise: r[1], dhuhr: r[2], asr: r[3], maghrib: r[4], isha: r[5] };
}

function nightPlan(isha, nextFajr, userBedtime = 1260, qiyamEnabled = true) {
  const bedtime = Math.max(userBedtime, isha + ISHA_TO_BED_MIN);
  const qiyam = qiyamEnabled && (nextFajr - WAKE_BEFORE_FAJR_QIYAM) - bedtime >= MIN_NIGHT_FOR_QIYAM;
  const wake = nextFajr - (qiyam ? WAKE_BEFORE_FAJR_QIYAM : WAKE_BEFORE_FAJR_PLAIN);
  const night = Math.max(0, wake - bedtime);
  const nap = NEED_SLEEP - night <= 30 ? SHORT_NAP : LONG_NAP;
  const total = night + nap;
  return { bedtime, wake, qiyam, witrBeforeSleep: !qiyam, night, nap, total, shortfall: Math.max(0, NEED_SLEEP - total) };
}

const B = (key, title, role, pref, min, priority, o = {}) =>
  ({ key, title, role, pref, min, priority, droppable: o.droppable !== false, flex: !!o.flex, deep: !!o.deep, isPrayer: !!o.prayer });
const REQ = { droppable: false };
function workdayTemplate(pNap) {
  return [
    ['FAJR', 'SUNRISE', [B('fajr', 'صلاة الفجر جماعة', 'WORSHIP', 25, 15, 1, { droppable: false, prayer: true }),
      B('ghadwa', 'ورد الغدوة: أذكار وقرآن، بلا تلفون', 'WORSHIP', 0, 15, 2, { droppable: false, flex: true })]],
    ['SUNRISE', 'DHUHR', [B('light', 'ضوء الشمس ومشي وفطور', 'BODY', 25, 10, 3),
      B('deep1', 'تركيز: أصعب مهمة', 'DEV', 90, 45, 2, { droppable: false, deep: true }),
      B('duha', 'استراحة وركعتا الضحى', 'WORSHIP', 20, 10, 4),
      B('deep2', 'تركيز: الكتلة الثانية', 'DEV', 90, 45, 5, { deep: true }),
      B('msgs1', 'نافذة الرسائل', 'BUSINESS', 20, 10, 4),
      B('work1', 'العمل: اتصالات ومتابعة', 'BUSINESS', 0, 0, 6, { flex: true })]],
    ['DHUHR', 'ASR', [B('dhuhr', 'صلاة الظهر وغداء خفيف', 'WORSHIP', 40, 25, 1, { droppable: false, prayer: true }),
      B('nap', `قيلولة ${pNap} دقيقة`, 'BODY', pNap, SHORT_NAP, 2, REQ),
      B('work2', 'العمل: ميدان واجتماعات', 'BUSINESS', 0, 0, 6, { flex: true })]],
    ['ASR', 'MAGHRIB', [B('asr', 'صلاة العصر', 'WORSHIP', 12, 10, 1, { droppable: false, prayer: true }),
      B('train', 'تدريب أو تعلم (حسب اليوم)', 'BODY', 60, 30, 4),
      B('close', 'الرسائل وإغلاق يوم العمل', 'BUSINESS', 20, 10, 5),
      B('child', 'وقت الابن وأذكار المساء', 'CHILD', 0, 30, 2, { droppable: false, flex: true })]],
    ['MAGHRIB', 'ISHA', [B('maghrib', 'صلاة المغرب', 'WORSHIP', 15, 10, 1, { droppable: false, prayer: true }),
      B('family', 'الأسرة: عشاء وحديث مع الزوجة', 'SPOUSE', 0, 30, 2, { droppable: false, flex: true })]],
    ['ISHA', 'BED', [B('isha', 'صلاة العشاء', 'WORSHIP', 15, 10, 1, { droppable: false, prayer: true }),
      B('self', 'وقت لك: قراءة أو تعلم خفيف', 'SELF', 0, 0, 6, { flex: true }),
      B('prep', 'الاستعداد للنوم: محاسبة وترتيب الغد', 'SELF', 30, 15, 3),
      B('witr', 'الوتر وأذكار النوم', 'WORSHIP', 15, 10, 1, REQ)]],
  ];
}
function fridayTemplate(pNap) {
  const t = workdayTemplate(pNap);
  t[1] = ['SUNRISE', 'DHUHR', [B('light', 'ضوء الشمس ومشي وفطور مع الأهل', 'BODY', 40, 15, 3),
    B('self_am', 'وقت لك أو للأسرة', 'SELF', 0, 0, 6, { flex: true }),
    B('jumua_prep', 'التبكير للجمعة: غُسل وسورة الكهف', 'WORSHIP', 60, 30, 1, REQ)]];
  t[2] = ['DHUHR', 'ASR', [B('jumua', 'الجمعة: الخطبة والصلاة', 'WORSHIP', 50, 40, 1, { droppable: false, prayer: true }),
    B('lunch', 'غداء مع الأسرة', 'SPOUSE', 40, 20, 3),
    B('nap', `قيلولة ${pNap} دقيقة`, 'BODY', pNap, SHORT_NAP, 2, REQ),
    B('visit', 'صلة الرحم ونزهة الأبناء', 'CHILD', 0, 0, 6, { flex: true })]];
  t[3] = ['ASR', 'MAGHRIB', [B('asr', 'صلاة العصر', 'WORSHIP', 12, 10, 1, { droppable: false, prayer: true }),
    B('free', 'فراغ مباح مع الأهل', 'SELF', 0, 0, 6, { flex: true }),
    B('dua', 'بلا تلفون: دعاء آخر ساعة من الجمعة', 'WORSHIP', 60, 30, 2, REQ)]];
  return t;
}

function fitSegment(start, end, blocks) {
  const notes = [], out = [];
  const avail = end - start;
  const dur = {};
  blocks.forEach((b) => { dur[b.key] = b.flex ? b.min : b.pref; });
  let live = blocks.slice();
  const total = () => live.reduce((s, b) => s + dur[b.key], 0);
  const byLowPriority = (arr) => arr.slice().sort((a, b) => b.priority - a.priority);
  const shrink = (pool) => {
    for (const b of byLowPriority(pool)) {
      const over = total() - avail;
      if (over <= 0) return;
      const cut = Math.min(over, dur[b.key] - b.min);
      if (cut > 0) { dur[b.key] -= cut; notes.push('shrunk:' + b.key); }
    }
  };
  shrink(live.filter((b) => b.droppable && !b.flex));
  for (const b of byLowPriority(live.filter((b) => b.droppable && !b.flex))) {
    if (total() - avail <= 0) break;
    live = live.filter((x) => x !== b);
    notes.push('dropped:' + b.key);
  }
  shrink(live.filter((b) => !b.droppable && !b.flex));
  const slack = avail - total();
  if (slack > 0) { const f = live.find((b) => b.flex); if (f) dur[f.key] += slack; }
  if (total() > avail) notes.push('overflow');
  let t = start;
  for (const b of live) {
    const d = dur[b.key];
    if (d <= 0) continue;
    out.push({ key: b.key, title: b.title, role: b.role, start: t, end: t + d, deep: b.deep, isPrayer: b.isPrayer,
      shrunkFrom: !b.flex && d < b.pref ? b.pref : null });
    t += d;
  }
  return { blocks: out, notes, dropped: blocks.filter((b) => !live.includes(b)) };
}

function weekdayOf(doy) { return new Date(Date.UTC(2026, 0, 1 + doy)).getUTCDay(); } // 0=Sun ... 5=Fri, 6=Sat

function resolveDay(cityKey, madhab, doy, userBedtime = 1260, qiyamEnabled = true) {
  const t = times(cityKey, madhab, doy);
  const nx = times(cityKey, madhab, doy + 1), pv = times(cityKey, madhab, doy - 1);
  const plan = nightPlan(t.isha, nx.fajr + 1440, userBedtime, qiyamEnabled);
  const prevPlan = nightPlan(pv.isha, t.fajr + 1440, userBedtime, qiyamEnabled);
  const friday = weekdayOf(doy) === 5;
  const tmpl = friday ? fridayTemplate(plan.nap) : workdayTemplate(plan.nap);
  const anchors = { FAJR: t.fajr, SUNRISE: t.sunrise, DHUHR: t.dhuhr, ASR: t.asr, MAGHRIB: t.maghrib, ISHA: t.isha, BED: plan.bedtime };
  const blocks = [], notes = [], dropped = [];
  if (prevPlan.qiyam) blocks.push({ key: 'qiyam', title: 'السَّحَر: ركعتان والوتر', role: 'WORSHIP', start: prevPlan.wake - 1440, end: t.fajr, deep: false, isPrayer: false, shrunkFrom: null });
  for (const [a, b, bl] of tmpl) {
    const seg = fitSegment(anchors[a], anchors[b], bl);
    blocks.push(...seg.blocks); notes.push(...seg.notes);
    seg.dropped.forEach((d) => dropped.push({ key: d.key, title: d.title, segment: a }));
  }
  return { doy, times: t, plan, prevPlan, blocks, notes, dropped, friday };
}

/* ---------- NOAA solar position ---------- */
const rad = (d) => d * Math.PI / 180, deg = (r) => r * 180 / Math.PI;
function sunPosition(lat, lon, utcMs) {
  const jd = utcMs / 86400000 + 2440587.5, T = (jd - 2451545) / 36525;
  const L0 = (280.46646 + T * (36000.76983 + T * 0.0003032)) % 360;
  const M = 357.52911 + T * (35999.05029 - 0.0001537 * T);
  const e = 0.016708634 - T * (0.000042037 + 0.0000001267 * T);
  const C = Math.sin(rad(M)) * (1.914602 - T * (0.004817 + 0.000014 * T)) + Math.sin(rad(2 * M)) * (0.019993 - 0.000101 * T) + Math.sin(rad(3 * M)) * 0.000289;
  const trueLong = L0 + C, omega = 125.04 - 1934.136 * T;
  const lambda = trueLong - 0.00569 - 0.00478 * Math.sin(rad(omega));
  const eps0 = 23 + (26 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60) / 60;
  const eps = eps0 + 0.00256 * Math.cos(rad(omega));
  const decl = deg(Math.asin(Math.sin(rad(eps)) * Math.sin(rad(lambda))));
  const y = Math.tan(rad(eps / 2)) ** 2;
  const eqTime = 4 * deg(y * Math.sin(2 * rad(L0)) - 2 * e * Math.sin(rad(M)) + 4 * e * y * Math.sin(rad(M)) * Math.cos(2 * rad(L0))
    - 0.5 * y * y * Math.sin(4 * rad(L0)) - 1.25 * e * e * Math.sin(2 * rad(M)));
  const d = new Date(utcMs);
  const utcMin = d.getUTCHours() * 60 + d.getUTCMinutes() + d.getUTCSeconds() / 60;
  let ha = (utcMin + eqTime + 4 * lon) / 4 - 180;
  if (ha < -180) ha += 360;
  const cosZ = Math.sin(rad(lat)) * Math.sin(rad(decl)) + Math.cos(rad(lat)) * Math.cos(rad(decl)) * Math.cos(rad(ha));
  const zen = deg(Math.acos(Math.max(-1, Math.min(1, cosZ))));
  const az = (deg(Math.atan2(Math.sin(rad(ha)), Math.cos(rad(ha)) * Math.sin(rad(lat)) - Math.tan(rad(decl)) * Math.cos(rad(lat)))) + 180 + 360) % 360;
  return { alt: 90 - zen, az, decl };
}
function localToUtc(cityKey, doy, minute) { return Date.UTC(2026, 0, 1 + doy) + (minute - utcOffset(cityKey, doy)) * 60000; }
function sunAt(cityKey, doy, minute) { const c = CITIES[cityKey]; return sunPosition(c.lat, c.lon, localToUtc(cityKey, doy, minute)); }

if (typeof module !== 'undefined') module.exports = { CITIES, times, nightPlan, resolveDay, fitSegment, sunAt, utcOffset, weekdayOf };
