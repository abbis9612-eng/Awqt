package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.decodeMyDay
import com.dcc.yawm.shared.data.decodeTasks
import com.dcc.yawm.shared.data.encodeMyDay
import com.dcc.yawm.shared.data.encodeTasks
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.PrayerSettings
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import com.dcc.yawm.shared.screen.dayView
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MyDayTest {
    private class City(val name: String, val place: GeoPoint, val zone: TimeZone, val prayer: PrayerSettings)

    private val cities = listOf(
        City("sulaymaniyah-shafi", GeoPoint(35.5613, 45.4375), TimeZone.of("Asia/Baghdad"), PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI)),
        City("sulaymaniyah-hanafi", GeoPoint(35.5613, 45.4375), TimeZone.of("Asia/Baghdad"), PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.HANAFI)),
        City(
            "london", GeoPoint(51.5074, -0.1278), TimeZone.of("Europe/London"),
            PrayerSettings(CalcMethod.MUSLIM_WORLD_LEAGUE, highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
        ),
    )

    private fun resolve(city: City, date: LocalDate, plan: MyDayPlan, cache: MutableMap<LocalDate, DayPrayerTimes>): ResolvedDay {
        fun t(d: LocalDate) = cache.getOrPut(d) { PrayerTimesEngine.compute(city.place, d, city.zone, city.prayer) }
        return DayResolver.resolve(t(date.minus(1, DateTimeUnit.DAY)), t(date), t(date.plus(1, DateTimeUnit.DAY)), myDay = plan)
    }

    private fun item(id: String, kind: String, anchor: ItemAnchor = MyDayCatalog.entry(kind)!!.anchor, at: Int = 17 * 60, minutes: Int? = null, important: Boolean? = null): MyItem {
        val e = MyDayCatalog.entry(kind)!!
        return MyItem(id, kind, e.title, e.icon, anchor, at, minutes ?: e.minutes, important ?: e.important)
    }

    /** Everything the owner might pick, with work hours and one fixed item that hits Maghrib in winter. */
    private val rich = MyDayPlan(
        life = LifeKind.JOB, workStart = 8 * 60, workEnd = 14 * 60,
        workDays = DayOfWeek.entries.toSet() - DayOfWeek.FRIDAY,
        items = MyDayCatalog.entries.mapIndexed { i, e ->
            val it = MyDayCatalog.newItem("m${i + 1}", e)
            if (e.kind == "sport") it.copy(anchor = ItemAnchor.FIXED, at = 17 * 60) else it
        },
    )

    private fun structural(d: ResolvedDay, plan: MyDayPlan, label: String) {
        val b = d.blocks
        assertTrue(b.all { it.end > it.start }, "$label: empty block")
        assertTrue(b.zipWithNext().all { (x, y) -> x.end <= y.start }, "$label: overlap ${b.zipWithNext().firstOrNull { (x, y) -> x.end > y.start }}")
        val t = d.times
        val at = mapOf("fajr" to t.fajr, "dhuhr" to t.dhuhr, "jumua" to t.dhuhr, "asr" to t.asr, "maghrib" to t.maghrib, "isha" to t.isha)
        assertEquals(5, b.count { it.isPrayer }, "$label: a prayer is missing")
        for (p in b.filter { it.isPrayer }) {
            assertEquals(at.getValue(p.key), p.start, "$label: ${p.key} not at its time")
            assertTrue(p.minutes >= 10, "$label: ${p.key} shorter than 10")
        }
        val prayers = listOf(t.fajr, t.dhuhr, t.asr, t.maghrib, t.isha)
        for (x in b) for (p in prayers) assertTrue(!(x.start < p && p < x.end), "$label: ${x.key} crosses a prayer")
        assertTrue(b.last().end <= d.plan.bedtime, "$label: runs past bedtime")
        assertTrue(d.adjustments.none { it.kind == AdjustmentKind.OVERFLOW }, "$label: overflow")
        // Nothing of the user's is longer than they asked.
        for (x in b) {
            val it = plan.items.firstOrNull { "my:${it.id}" == x.key || "fixed:${it.id}" == x.key } ?: continue
            assertTrue(x.minutes <= it.minutes, "$label: ${x.key} longer than chosen")
        }
        // An important item shortens only when no ordinary item is left in its segment.
        val segments = myDayTemplate(d.type, plan, d.plan.witrBeforeSleep)
        for (seg in segments) {
            val keys = seg.blocks.map { it.key }.toSet()
            val inSeg = b.filter { it.key in keys }
            val shrunkImportant = inSeg.any { x -> x.shrunkFromMinutes != null && seg.blocks.first { it.key == x.key }.let { !it.droppable && !it.isPrayer && it.key != "witr" } }
            val normalLeft = inSeg.any { x -> seg.blocks.first { it.key == x.key }.droppable && !x.key.startsWith("free:") }
            assertTrue(!(shrunkImportant && normalLeft), "$label: an important item shrank while an ordinary one is still there")
        }
    }

    @Test
    fun everyDayOfTheYearHoldsInThreeCities() {
        for (city in cities) {
            val cache = HashMap<LocalDate, DayPrayerTimes>()
            var date = LocalDate(2026, 1, 1)
            while (date.year == 2026) {
                structural(resolve(city, date, rich, cache), rich, "${city.name} $date")
                structural(resolve(city, date, MyDayPlan(), cache), MyDayPlan(), "${city.name} $date empty")
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    @Test
    fun anEmptyPlanIsPrayersFreeTimeAndTheNight() {
        val d = resolve(cities[0], LocalDate(2026, 10, 29), MyDayPlan(), HashMap())
        assertTrue(d.blocks.all { it.isPrayer || it.key.startsWith("free:") || it.key == "witr" || it.key == "qiyam" }, d.blocks.map { it.key }.toString())
        assertTrue(d.blocks.any { it.key.startsWith("free:") })
    }

    @Test
    fun anItemAfterAsrMovesWithAsrAllYear() {
        val plan = MyDayPlan(items = listOf(item("m1", "kids", ItemAnchor.ASR, minutes = 45)))
        val cache = HashMap<LocalDate, DayPrayerTimes>()
        var date = LocalDate(2026, 1, 1)
        while (date.year == 2026) {
            val d = resolve(cities[0], date, plan, cache)
            val asr = d.blocks.first { it.key == "asr" }
            val kids = d.blocks.first { it.key == "my:m1" }
            assertEquals(asr.end, kids.start, "$date: not right after Asr")
            assertEquals(45, kids.minutes, "$date: shortened without need")
            date = date.plus(1, DateTimeUnit.DAY)
        }
    }

    @Test
    fun aFixedItemKeepsItsClockTime() {
        val plan = MyDayPlan(items = listOf(item("m1", "study", ItemAnchor.FIXED, at = 10 * 60, minutes = 45)))
        val d = resolve(cities[0], LocalDate(2026, 6, 21), plan, HashMap())
        val b = d.blocks.first { it.key == "fixed:m1" }
        assertEquals(600, b.start)
        assertEquals(645, b.end)
    }

    @Test
    fun workIsCutAroundTheNoonPrayer() {
        val plan = MyDayPlan(life = LifeKind.JOB, workStart = 8 * 60, workEnd = 14 * 60, workDays = DayOfWeek.entries.toSet())
        val d = resolve(cities[0], LocalDate(2026, 10, 29), plan, HashMap())
        val work = d.blocks.filter { it.key == "fixed:work" }
        assertEquals(2, work.size, d.blocks.map { "${it.key} ${it.start}-${it.end}" }.toString())
        val dhuhr = d.blocks.first { it.key == "dhuhr" }
        assertEquals(dhuhr.start, work[0].end)
        assertEquals(dhuhr.end, work[1].start)
        assertEquals(14 * 60, work[1].end)
    }

    @Test
    fun ordinaryItemsGiveWayBeforeImportantOnes() {
        // 29 Oct in Sulaymaniyah: Maghrib 17:06 to Isha 18:30, 69 free minutes after the prayer.
        val plan = MyDayPlan(items = listOf(
            item("m1", "family", ItemAnchor.MAGHRIB, minutes = 60, important = true),
            item("m2", "sport", ItemAnchor.MAGHRIB, minutes = 60, important = false),
            item("m3", "walk", ItemAnchor.MAGHRIB, minutes = 45, important = false),
        ))
        val d = resolve(cities[0], LocalDate(2026, 10, 29), plan, HashMap())
        val family = d.blocks.first { it.key == "my:m1" }
        assertEquals(60, family.minutes)
        assertNull(family.shrunkFromMinutes)
        assertTrue(d.blocks.none { it.key == "my:m2" || it.key == "my:m3" }, d.blocks.map { it.key }.toString())
        assertTrue(d.adjustments.any { it.kind == AdjustmentKind.DROPPED && it.key == "my:m2" })
        val view = dayView(UserSettings(myDay = plan), LocalDate(2026, 10, 29))
        assertTrue(view.changes.any { "رياضة" in it && "بعد المغرب" in it }, view.changes.toString())
        assertTrue(view.items.any { it.key.startsWith("free:") && it.title == FREE_TITLE && it.icon == "palm" })
    }

    @Test
    fun withoutAPlanTheTemplateIsUntouched() {
        val city = cities[0]
        val cache = HashMap<LocalDate, DayPrayerTimes>()
        val date = LocalDate(2026, 10, 29)
        fun t(d: LocalDate) = cache.getOrPut(d) { PrayerTimesEngine.compute(city.place, d, city.zone, city.prayer) }
        val a = DayResolver.resolve(t(date.minus(1, DateTimeUnit.DAY)), t(date), t(date.plus(1, DateTimeUnit.DAY)))
        val b = DayResolver.resolve(t(date.minus(1, DateTimeUnit.DAY)), t(date), t(date.plus(1, DateTimeUnit.DAY)), myDay = null)
        assertEquals(a, b)
        assertTrue(a.blocks.any { it.key == "deep1" })
    }

    // ---- tasks ----

    private val today = LocalDate(2026, 10, 29)

    @Test
    fun tasksNeverOverlapAndPinnedOnesKeepTheirTime() {
        val slots = listOf(Slot("free:fajr", 300, 420), Slot("free:asr", 900, 1000))
        val tasks = listOf(
            TaskItem("t1", "أ", 30, today),
            TaskItem("t2", "ب", 45, today, pinAt = 330),
            TaskItem("t3", "ج", 60, today),
            TaskItem("t4", "د", 120, today),
            TaskItem("t5", "هـ", 15, today, doneOn = today),
        )
        val p = placeTasks(tasks, slots, from = 290)
        val byId = p.associateBy { it.task.id }
        assertEquals(330, byId.getValue("t2").start)
        assertTrue(byId.getValue("t2").pinned)
        assertEquals(300, byId.getValue("t1").start)
        assertTrue(byId.getValue("t4").noRoom)
        assertNull(byId.getValue("t5").start)
        val spans = p.filter { it.start != null }.map { it.start!! until it.start + it.task.minutes }.sortedBy { it.first }
        assertTrue(spans.zipWithNext().all { (x, y) -> x.last < y.first }, spans.toString())
        for (x in p.filter { it.start != null && !it.pinned }) {
            val s = slots.first { it.key == x.slotKey }
            assertTrue(x.start!! >= s.start && x.start + x.task.minutes <= s.end)
        }
    }

    @Test
    fun openTasksFromEarlierDaysComeToToday() {
        val tasks = listOf(
            TaskItem("t1", "قديمة", 30, LocalDate(2026, 10, 27)),
            TaskItem("t2", "خلصت", 30, LocalDate(2026, 10, 27), doneOn = LocalDate(2026, 10, 27)),
            TaskItem("t3", "باچر", 30, LocalDate(2026, 10, 30)),
            TaskItem("t4", "بالبال", 30, null),
        )
        assertEquals(listOf("t1"), tasksOn(today, today, tasks).map { it.id })
        assertEquals(listOf("t3"), tasksOn(LocalDate(2026, 10, 30), today, tasks).map { it.id })
    }

    @Test
    fun quickAddUnderstandsEverydayWords() {
        assertEquals(ParsedTask("اتصل بالمحاسب", TaskWhen.TOMORROW, 30), parseTask("اتصل بالمحاسب باچر نص ساعة"))
        assertEquals(ParsedTask("اكتب التقرير", null, 45), parseTask("اكتب التقرير 45 د"))
        assertEquals(ParsedTask("رياضة", null, 90), parseTask("رياضة ساعة ونص"))
        assertEquals(ParsedTask("شي", TaskWhen.LATER, null), parseTask("شي بعدين"))
        assertEquals(ParsedTask("درس", null, 120), parseTask("درس ٢ ساعة"))
        assertEquals(ParsedTask("زيارة الساعة", null, null), parseTask("زيارة الساعة"))
    }

    // ---- storage ----

    @Test
    fun theDayAndTasksSurviveSavingAndBadRecordsAreSkipped() {
        assertEquals(rich, decodeMyDay(encodeMyDay(rich)))
        assertNull(decodeMyDay(encodeMyDay(null)))
        val tasks = listOf(TaskItem("t1", "أ", 30, today, pinAt = 600), TaskItem("t2", "ب", 15, null, doneOn = today))
        assertEquals(tasks, decodeTasks(encodeTasks(tasks)))
        val broken = encodeTasks(tasks) + "\u001ET\u001Fx\u001F\u001F99999\u001F\u001F\u001F"
        assertEquals(tasks, decodeTasks(broken))
        val day = decodeMyDay("L\u001FJOB\u001F480\u001F840\u001F1,2\u001EI\u001Fm1\u001Fsport\u001F\u001Fdumbbell\u001FASR\u001F1020\u001F45\u001Ffalse")
        assertEquals(LifeKind.JOB, day!!.life)
        assertTrue(day.items.isEmpty())
    }
}
