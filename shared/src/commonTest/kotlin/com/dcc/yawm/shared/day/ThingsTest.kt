package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.decodeSettings
import com.dcc.yawm.shared.data.decodeThings
import com.dcc.yawm.shared.data.encodeMyDay
import com.dcc.yawm.shared.data.encodeSettings
import com.dcc.yawm.shared.data.encodeTasks
import com.dcc.yawm.shared.data.encodeThings
import com.dcc.yawm.shared.alarm.AlertPrefs
import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.screen.AddTaps
import com.dcc.yawm.shared.screen.Where
import com.dcc.yawm.shared.screen.addDraft
import com.dcc.yawm.shared.screen.partsView
import com.dcc.yawm.shared.screen.savedMessage
import com.dcc.yawm.shared.screen.toAlarm
import com.dcc.yawm.shared.screen.toThing
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThingsTest {
    private val s = UserSettings()
    private val zone = TimeZone.of(s.zoneId)
    private val monday = LocalDate(2026, 11, 2)
    private fun times(d: LocalDate) = PrayerTimesEngine.compute(s.place, d, zone, s.prayer)

    @Test
    fun sixPartsBetweenThePrayersCoverTheDayEveryDayOfTheYear() {
        var d = LocalDate(2026, 1, 1)
        while (d.year == 2026) {
            val t = times(d)
            val night = nightPlan(t.wallMinutes(Prayer.ISHA), times(d.plus(1, DateTimeUnit.DAY)).wallMinutes(Prayer.FAJR) + 1440, s.bedtime)
            val spans = daySpans(t, night.bedtime)
            assertEquals(DayPart.entries, spans.map { it.part })
            assertEquals(t.wallMinutes(Prayer.FAJR), spans.first().start)
            spans.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start, "$d ${a.part}") }
            assertTrue(spans.all { it.end > it.start }, "$d")
            assertEquals(night.bedtime, spans.last().end, "$d: the last part ends at bedtime")
            d = d.plus(1, DateTimeUnit.DAY)
        }
        // 2 November 2026 in Sulaymaniyah (docs/reference/reference-2026.csv): Asr 14:39, Maghrib 17:02.
        val spans = daySpans(times(monday), 21 * 60)
        val asr = spans.first { it.part == DayPart.ASR }
        assertEquals(14 * 60 + 39, asr.start)
        assertEquals(17 * 60 + 2, asr.end)
        assertEquals(DayPart.ASR, partAt(15 * 60 + 10, spans)?.part)
        assertNull(partAt(22 * 60, spans), "after bedtime is the night")
    }

    @Test
    fun whichThingsFallOnADay() {
        val friday = LocalDate(2026, 11, 6)
        val list = listOf(
            Thing("h1", "قرآن", DayPart.FAJR, repeat = Repeat.DAILY),
            Thing("h2", "الدوام", at = 480, end = 840, repeat = Repeat.WORKDAYS),
            Thing("h3", "المصلّح", date = monday.plus(1, DateTimeUnit.DAY)),
            Thing("h4", "فاتورة", date = LocalDate(2026, 10, 30)),
            Thing("h5", "إجازة السوق"),
            Thing("h6", "هدية", date = LocalDate(2026, 10, 31), done = setOf(LocalDate(2026, 10, 31))),
        )
        assertEquals(listOf("h1", "h2", "h4"), thingsOn(monday, monday, list, DefaultWorkdays).map { it.id }, "open old one-offs come along to today")
        assertEquals(listOf("h1", "h2", "h3"), thingsOn(monday.plus(1, DateTimeUnit.DAY), monday, list, DefaultWorkdays).map { it.id })
        assertEquals(listOf("h1", "h3"), thingsOn(friday, friday, list.filter { it.id != "h4" }, DefaultWorkdays).map { it.id }, "no work on Friday; Tuesday's open one comes along")
        assertTrue(list[4].isLater)
    }

    @Test
    fun doneIsPerDayForWhatRepeats() {
        val q = Thing("h1", "قرآن", DayPart.FAJR, repeat = Repeat.DAILY)
        val done = q.toggled(monday)
        assertTrue(done.doneOn(monday))
        assertFalse(done.doneOn(monday.plus(1, DateTimeUnit.DAY)))
        assertFalse(done.toggled(monday).doneOn(monday))
        // A long-running daily thing keeps only the last four weeks.
        var t = q
        for (i in 0 until 60) t = t.toggled(LocalDate(2026, 9, 1).plus(i, DateTimeUnit.DAY))
        assertTrue(t.done.size <= 28)
        val once = Thing("h2", "المصلّح", date = monday).toggled(monday)
        assertTrue(once.doneOn(monday.plus(3, DateTimeUnit.DAY)), "a one-off stays done")
    }

    @Test
    fun aThingShowsInItsPartOrThePartOfItsTimeOrTheCurrentPart() {
        val spans = daySpans(times(monday), 21 * 60)
        assertEquals(DayPart.ISHA, partOf(Thing("a", "دراسة", DayPart.ISHA), spans, 600))
        assertEquals(DayPart.MORNING, partOf(Thing("b", "الدوام", at = 480), spans, null))
        assertEquals(DayPart.ASR, partOf(Thing("c", "اتصل", date = monday), spans, 15 * 60 + 10), "any time today: the part of now")
        assertEquals(DayPart.FAJR, partOf(Thing("d", "دواء", at = 4 * 60), spans, null), "before Fajr: with the first part")
    }

    @Test
    fun typedInEverydayWords() {
        fun p(s: String) = parseThing(s)
        assertEquals(ParsedThing("قراءة كتاب", DayPart.ISHA, repeat = Repeat.DAILY), p("قراءة كتاب بعد العشاء كل يوم"))
        assertEquals(ParsedThing("زيارة خالي", DayPart.MAGHRIB), p("زيارة خالي بعد المغرب"))
        assertEquals(ParsedThing("اتصل بالمصلّح", whenDay = TaskWhen.TOMORROW), p("اتصل بالمصلّح باچر"))
        assertEquals(ParsedThing("منبّه", at = 5 * 60 + 45, alarm = true), p("منبّه الساعة 05:45"))
        assertEquals(ParsedThing("الدواء", at = 21 * 60, alarm = true), p("صحّيني الدواء الساعة 9 بالليل"))
        assertEquals(ParsedThing("دواء", at = 7 * 60), p("دواء الساعة ٧ الصبح"))
        assertEquals(ParsedThing("رياضة", DayPart.ASR, repeat = Repeat.WORKDAYS), p("رياضة العصر أيام الدوام"))
        assertEquals(ParsedThing("تصليح السيارة", whenDay = TaskWhen.LATER), p("تصليح السيارة بعدين"))
        assertEquals(ParsedThing("دواء", at = 21 * 60), p("دواء ٢١:٠٠"))
    }

    @Test
    fun theOldDayAndTasksBecomeThingsOnceAndNothingIsLost() {
        val plan = MyDayPlan(
            life = LifeKind.JOB, workStart = 8 * 60, workEnd = 14 * 60, workDays = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
            items = listOf(
                MyItem("m1", "sport", "رياضة", "dumbbell", ItemAnchor.ASR, minutes = 45, important = false),
                MyItem("m2", "own", "درس", "book", ItemAnchor.FIXED, at = 19 * 60, minutes = 60, important = true),
            ),
        )
        val tasks = listOf(TaskItem("t1", "اتصل بالمحاسب", 15, monday), TaskItem("t2", "زيارة", 30, monday, pinAt = 17 * 60 + 40, doneOn = monday))
        val stored = encodeSettings(UserSettings(myDay = plan, tasks = tasks)).toMutableMap()
        stored.remove("things.v1")
        stored.remove("work.days")
        val s1 = decodeSettings(stored)
        assertEquals(listOf("الدوام", "رياضة", "درس", "اتصل بالمحاسب", "زيارة"), s1.things.map { it.title })
        assertEquals(Repeat.WORKDAYS, s1.things[0].repeat)
        assertEquals(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), s1.workDays)
        assertEquals(DayPart.ASR, s1.things[1].part)
        assertEquals(19 * 60, s1.things[2].at)
        assertEquals(Remind.AT, s1.things[4].remind)
        assertTrue(s1.things[4].doneOn(monday))
        assertEquals(s1.things.size, s1.things.map { it.id }.toSet().size)
        // Saved once, the things are kept as they are (not rebuilt from the old data).
        val s2 = decodeSettings(encodeSettings(s1.copy(things = s1.things.drop(1))))
        assertEquals(4, s2.things.size)
        assertEquals(encodeTasks(tasks), encodeSettings(s2)["tasks.v1"], "the old data stays stored, untouched")
        assertEquals(encodeMyDay(plan), encodeSettings(s2)["day.mine.v1"])
    }

    @Test
    fun thingsSurviveSavingAndBadRecordsAreSkipped() {
        val list = listOf(
            Thing("h1", "قرآن", DayPart.FAJR, repeat = Repeat.DAILY, done = setOf(monday)),
            Thing("h2", "الدوام", at = 480, end = 840, repeat = Repeat.WORKDAYS, remind = Remind.NONE),
            Thing("h3", "زيارة", date = monday, at = 1060, remind = Remind.AT),
            Thing("h4", "إجازة السوق"),
        )
        assertEquals(list, decodeThings(encodeThings(list)))
        assertEquals(listOf("h1"), decodeThings(encodeThings(list.take(1)) + "\u001Ejunk\u001EH\u001Fx").map { it.id })
        assertEquals("h5", nextThingId(list))
    }

    @Test
    fun theOnePageOfTheDay() {
        val things = listOf(
            Thing("h1", "رياضة", DayPart.ASR, repeat = Repeat.DAILY),
            Thing("h2", "اتصل بالمحاسب", date = monday),
            Thing("h3", "زيارة خالي", date = monday.plus(1, DateTimeUnit.DAY)),
            Thing("h4", "إجازة السوق"),
            Thing("h5", "الدوام", at = 480, end = 840, repeat = Repeat.WORKDAYS),
        )
        val alarms = listOf(UserAlarm("a1", "دواء", 420), UserAlarm("a2", "مرة", 345, onceOn = LocalDate(2026, 12, 1)))
        val st = s.copy(things = things, alerts = AlertPrefs(alarms = alarms))
        val v = partsView(st, monday, monday, 15 * 60 + 10)
        assertEquals(DayPart.ASR, v.current(15 * 60 + 10)?.part)
        assertEquals(listOf("اتصل بالمحاسب", "رياضة"), v.current(15 * 60 + 10)!!.things.map { it.title }, "any time today joins the part of now")
        assertEquals(listOf("الدوام"), v.parts.first { it.part == DayPart.MORNING }.things.map { it.title })
        assertEquals(listOf(DayPart.FAJR, DayPart.MORNING, DayPart.DHUHR), v.past(15 * 60 + 10).map { it.part })
        assertEquals(listOf(DayPart.MAGHRIB, DayPart.ISHA), v.future(15 * 60 + 10).map { it.part })
        assertEquals(listOf("زيارة خالي"), v.tomorrow.map { it.title })
        assertEquals(listOf("إجازة السوق"), v.later.map { it.title })
        assertEquals(listOf("a1"), v.alarms.map { it.id }, "a one-time alarm of another day is not tonight's")
        assertTrue(v.asleep(21 * 60 + 40))
        assertTrue(v.asleep(1440 + 3 * 60), "after midnight, before Fajr")
        assertEquals("asr", v.prayerKey(DayPart.ASR))
        assertEquals(null, v.prayerKey(DayPart.MORNING))
        assertEquals("jumua", partsView(st, LocalDate(2026, 11, 6), monday, null).prayerKey(DayPart.DHUHR))
    }

    @Test
    fun theAddSheetFollowsTheWordsUnlessTapped() {
        val spans = daySpans(times(monday), 21 * 60)
        val now = 15 * 60 + 10
        fun d(raw: String, taps: AddTaps = AddTaps(), alarm: Boolean = false) = addDraft(raw, alarm, taps, DayPart.ASR, spans, now)
        val read = d("قراءة كتاب بعد العشاء", AddTaps(repeat = Repeat.DAILY))
        assertEquals(Where.In(DayPart.ISHA), read.where)
        assertEquals(Thing("h9", "قراءة كتاب", DayPart.ISHA, monday, repeat = Repeat.DAILY), read.toThing("h9", monday))
        assertEquals("انحطّت: بعد العشاء · كل يوم", read.savedMessage())
        assertEquals(Where.In(DayPart.ASR), d("اتصل بالمحاسب").where, "no words: the part of now")
        assertEquals(Where.Tomorrow, d("اتصل بالمصلّح باچر").where)
        assertEquals(monday.plus(1, DateTimeUnit.DAY), d("اتصل بالمصلّح باچر").toThing("h1", monday).date)
        val later = d("تصليح السيارة بعدين")
        assertTrue(later.toThing("h1", monday).isLater)
        // A time puts it in the part of that time and rings then.
        val visit = d("زيارة الساعة 17:40")
        assertEquals(Where.In(DayPart.MAGHRIB), visit.where)
        assertEquals(Remind.AT, visit.remind)
        assertNull(visit.toThing("h1", monday).part)
        // Alarms: daily by default; a passed time is said honestly.
        val alarm = d("منبّه الساعة 05:45")
        assertTrue(alarm.alarm && alarm.passed)
        assertEquals(Repeat.DAILY, alarm.repeat)
        assertEquals("المنبّه 05:45 · كل يوم", alarm.savedMessage())
        val once = d("منبّه الساعة 05:45", AddTaps(repeat = Repeat.ONCE))
        assertEquals(monday.plus(1, DateTimeUnit.DAY), once.toAlarm("a1", DefaultWorkdays, monday, now).onceOn)
        assertEquals("المنبّه 05:45 · مرة وحدة · يرن باچر", once.savedMessage())
        assertEquals(7 * 60, d("", alarm = true).at, "the alarm sheet starts at 07:00")
        assertEquals(5 * 60 + 30, d("", AddTaps(at = 330), alarm = true).at)
    }
}
