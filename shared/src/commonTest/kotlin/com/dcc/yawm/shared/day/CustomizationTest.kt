package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.data.cleanTitle
import com.dcc.yawm.shared.data.decodeCustomization
import com.dcc.yawm.shared.data.encodeCustomization
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.PrayerSettings
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomizationTest {
    private class City(val place: GeoPoint, val zone: TimeZone, val prayer: PrayerSettings)

    private val sulaymaniyah = City(GeoPoint(35.5613, 45.4375), TimeZone.of("Asia/Baghdad"), PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI))
    private val hanafi = City(GeoPoint(35.5613, 45.4375), TimeZone.of("Asia/Baghdad"), PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.HANAFI))
    private val london = City(
        GeoPoint(51.5074, -0.1278), TimeZone.of("Europe/London"),
        PrayerSettings(CalcMethod.MUSLIM_WORLD_LEAGUE, highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
    )

    private fun resolve(city: City, date: LocalDate, c: DayCustomization, cache: MutableMap<LocalDate, DayPrayerTimes> = HashMap()): ResolvedDay {
        fun t(d: LocalDate) = cache.getOrPut(d) { PrayerTimesEngine.compute(city.place, d, city.zone, city.prayer) }
        return DayResolver.resolve(t(date.minus(1, DateTimeUnit.DAY)), t(date), t(date.plus(1, DateTimeUnit.DAY)), custom = c)
    }

    private val tuesday = LocalDate(2026, 10, 27)
    private val thursday = LocalDate(2026, 10, 29)

    /** Ordered, no overlap, every block longer than zero, prayers exactly at their time and ≥ 10 min. */
    private fun structural(d: ResolvedDay, label: String) {
        val b = d.blocks
        assertTrue(b.all { it.end > it.start }, "$label: empty block")
        assertTrue(b.zipWithNext().all { (x, y) -> x.end <= y.start }, "$label: overlap ${b.zipWithNext().firstOrNull { (x, y) -> x.end > y.start }}")
        val times = mapOf("fajr" to d.times.fajr, "dhuhr" to d.times.dhuhr, "jumua" to d.times.dhuhr, "asr" to d.times.asr, "maghrib" to d.times.maghrib, "isha" to d.times.isha)
        for (p in b.filter { it.isPrayer }) {
            assertEquals(times.getValue(p.key), p.start, "$label: ${p.key} not at its time")
            assertTrue(p.end - p.start >= 10, "$label: ${p.key} shorter than 10")
        }
        assertEquals(5, b.count { it.isPrayer }, "$label: a prayer is missing")
        assertTrue(b.last().end <= d.plan.bedtime, "$label: runs past bedtime")
    }

    @Test
    fun noCustomizationIsTheReferenceTemplate() {
        val empty = DayCustomization()
        for (type in DayType.entries) assertEquals(Templates.forDay(type, 20), customizedTemplate(type, 20, empty))
        assertEquals(empty, decodeCustomization(encodeCustomization(empty)))
        assertEquals(empty, decodeCustomization(null))
        for (day in DayOfWeek.entries) {
            assertEquals(if (day == DayOfWeek.FRIDAY) DayType.FRIDAY else DayType.WORKDAY, empty.dayType(day))
        }
    }

    @Test
    fun renameResizeHideAndReorder() {
        val c = DayCustomization(
            changes = mapOf(DayType.WORKDAY to mapOf(
                "deep2" to BlockChange(hidden = true),
                "light" to BlockChange(title = "مشي الصبح", minutes = 40),
                "fajr" to BlockChange(hidden = true), // prayers cannot be hidden
            )),
            order = mapOf(DayType.WORKDAY to mapOf(Anchor.SUNRISE to listOf("msgs1", "deep1", "light"))),
        )
        val d = resolve(sulaymaniyah, tuesday, c)
        structural(d, "edited")
        assertNull(d.blocks.firstOrNull { it.key == "deep2" })
        assertNotNull(d.blocks.firstOrNull { it.key == "fajr" })
        val light = d.blocks.first { it.key == "light" }
        assertEquals("مشي الصبح", light.title)
        assertEquals(40, light.end - light.start)
        val morning = d.blocks.filter { it.start >= d.times.sunrise && it.end <= d.times.dhuhr }.map { it.key }
        assertEquals(listOf("msgs1", "deep1", "light"), morning.take(3))
    }

    @Test
    fun ownBlocksSitInTheirSegment() {
        val c = DayCustomization(custom = mapOf(DayType.WORKDAY to listOf(
            CustomBlock("u1", Anchor.ASR, "دراسة", 45, Importance.MUST, Role.LEARNING),
            CustomBlock("u2", Anchor.ISHA, "اتصال بأمي", 15, Importance.NORMAL),
        )))
        assertEquals("u3", c.nextCustomKey())
        for (date in listOf(tuesday, LocalDate(2026, 6, 16), LocalDate(2026, 12, 22))) {
            val d = resolve(sulaymaniyah, date, c)
            structural(d, "own $date")
            val study = d.blocks.first { it.key == "u1" }
            assertTrue(study.start >= d.times.asr && study.end <= d.times.maghrib)
            assertTrue(study.end - study.start >= 22, "a MUST block keeps at least half")
        }
    }

    @Test
    fun aDayOffUsesTheDayOffTemplate() {
        val c = DayCustomization(workdays = DefaultWorkdays - DayOfWeek.THURSDAY)
        assertEquals(DayType.OFFDAY, c.dayType(DayOfWeek.THURSDAY))
        val d = resolve(sulaymaniyah, thursday, c)
        structural(d, "day off")
        assertEquals(DayType.OFFDAY, d.type)
        assertTrue(d.blocks.none { it.isDeepWork || it.key == "jumua" || it.key == "jumua_prep" || it.key == "dua" })
        assertEquals("صلاة الظهر", d.blocks.first { it.key == "dhuhr" }.title)
        assertEquals(DayType.FRIDAY, c.dayType(DayOfWeek.FRIDAY))
    }

    private val work = FixedAppointment(
        "w", "الدوام", 8 * 60, 14 * 60,
        setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
    )

    @Test
    fun theDayArrangesItselfAroundAFixedAppointment() {
        val c = DayCustomization(appointments = listOf(work))
        val d = resolve(sulaymaniyah, tuesday, c)
        structural(d, "appointment")
        val fixed = d.blocks.filter { it.key == "fixed:w" }
        assertTrue(fixed.isNotEmpty())
        // Only the appointment and the prayer that falls inside it use 08:00–14:00.
        val inside = d.blocks.filter { it.start < work.end && it.end > work.start }
        assertTrue(inside.all { it.key == "fixed:w" || it.isPrayer }, inside.toString())
        // Dhuhr at work: the prayer keeps its time and is shortened to its minimum.
        val dhuhr = d.blocks.first { it.key == "dhuhr" }
        assertEquals(25, dhuhr.end - dhuhr.start)
        assertEquals(work.end, fixed.last().end)
        // Friday has no appointment.
        assertTrue(resolve(sulaymaniyah, LocalDate(2026, 10, 30), c).blocks.none { it.key.startsWith("fixed:") })
    }

    @Test
    fun everyDayOfTheYearStaysSoundWithChanges() {
        val c = DayCustomization(
            changes = mapOf(DayType.WORKDAY to mapOf("train" to BlockChange(minutes = 90), "msgs1" to BlockChange(hidden = true))),
            custom = mapOf(
                DayType.WORKDAY to listOf(CustomBlock("u1", Anchor.MAGHRIB, "دراسة", 60, Importance.MUST)),
                DayType.OFFDAY to listOf(CustomBlock("u2", Anchor.SUNRISE, "سوق", 90, Importance.IF_TIME)),
            ),
            appointments = listOf(work, FixedAppointment("g", "النادي", 17 * 60, 18 * 60 + 30, setOf(DayOfWeek.SATURDAY, DayOfWeek.MONDAY))),
            workdays = DefaultWorkdays - DayOfWeek.SATURDAY,
        )
        for (city in listOf(sulaymaniyah, hanafi, london)) {
            val cache = HashMap<LocalDate, DayPrayerTimes>()
            var date = LocalDate(2026, 1, 1)
            while (date.year == 2026) {
                val d = resolve(city, date, c, cache)
                structural(d, "$date")
                for (a in c.appointmentsOn(date.dayOfWeek)) {
                    val clash = d.blocks.filter { !it.isPrayer && !it.key.startsWith("fixed:") && it.start < a.end && it.end > a.start }
                    assertTrue(clash.isEmpty(), "$date ${a.title}: $clash")
                }
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    @Test
    fun randomChangesNeverBreakTheDay() {
        val rnd = Random(2026)
        val keys = Templates.workday(20).flatMap { it.blocks }.map { it.key }
        repeat(120) { round ->
            val c = DayCustomization(
                changes = mapOf(DayType.WORKDAY to keys.filter { rnd.nextInt(3) == 0 }.associateWith {
                    BlockChange(minutes = rnd.nextInt(5, 200), hidden = rnd.nextInt(4) == 0)
                }),
                order = mapOf(DayType.WORKDAY to mapOf(Anchor.SUNRISE to keys.shuffled(rnd))),
                custom = mapOf(DayType.WORKDAY to List(rnd.nextInt(0, 4)) {
                    CustomBlock("u$it", Anchor.entries[rnd.nextInt(0, 6)], "شي $it", rnd.nextInt(5, 240), Importance.entries.random(rnd))
                }),
                appointments = List(rnd.nextInt(0, 3)) {
                    val s = rnd.nextInt(5 * 60, 20 * 60)
                    FixedAppointment("a$it", "موعد", s, (s + rnd.nextInt(15, 300)).coerceAtMost(23 * 60), DayOfWeek.entries.toSet())
                },
            )
            val date = LocalDate(2026, 1, 1).plus(rnd.nextInt(0, 365), DateTimeUnit.DAY)
            for (city in listOf(sulaymaniyah, london)) structural(resolve(city, date, c), "round $round $date")
        }
    }

    @Test
    fun storedCustomizationRoundTripsAndSkipsJunk() {
        val c = DayCustomization(
            changes = mapOf(DayType.WORKDAY to mapOf("light" to BlockChange("مشي", 40, false), "deep2" to BlockChange(hidden = true))),
            order = mapOf(DayType.OFFDAY to mapOf(Anchor.SUNRISE to listOf("self_am", "light"))),
            custom = mapOf(DayType.FRIDAY to listOf(CustomBlock("u1", Anchor.ASR, "زيارة", 30, Importance.IF_TIME, Role.CHILD))),
            appointments = listOf(work),
            workdays = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
        )
        assertEquals(c, decodeCustomization(encodeCustomization(c)))
        val junk = encodeCustomization(c) + "\u001EX\u001Fnonsense\u001EA\u001Fbad\u001Ft\u001F900\u001F100\u001F1\u001FSELF"
        assertEquals(c, decodeCustomization(junk))
        assertEquals("موعد مهم", cleanTitle("  موعد\u001E مهم\u001F\n"))
    }
}

class CustomizationEditsTest {
    @Test
    fun editsDoWhatTheScreenSays() {
        var c = DayCustomization()
        c = c.withChange(DayType.WORKDAY, "deep2") { it.copy(hidden = true) }
        val morning = c.editorView(DayType.WORKDAY).first { it.first.from == Anchor.SUNRISE }.second
        assertTrue(morning.first { it.spec.key == "deep2" }.hidden, "hidden blocks stay listed")

        c = c.moved(DayType.WORKDAY, Anchor.SUNRISE, "deep1", -1)
        val keys = c.editorView(DayType.WORKDAY).first { it.first.from == Anchor.SUNRISE }.second.map { it.spec.key }
        assertEquals(listOf("deep1", "light"), keys.take(2))
        assertEquals(c, c.moved(DayType.WORKDAY, Anchor.DHUHR, "dhuhr", 1), "prayers do not move")

        c = c.withOwnBlock(DayType.WORKDAY, CustomBlock(c.nextCustomKey(), Anchor.ASR, "دراسة", 45, Importance.MUST))
        assertTrue(c.editorView(DayType.WORKDAY).flatMap { it.second }.any { it.isOwn && it.spec.title == "دراسة" })
        c = c.withoutOwnBlock(DayType.WORKDAY, "u1")
        assertTrue(c.custom[DayType.WORKDAY].orEmpty().isEmpty())

        c = c.toggledWorkday(DayOfWeek.SATURDAY).toggledWorkday(DayOfWeek.FRIDAY)
        assertEquals(DayType.OFFDAY, c.dayType(DayOfWeek.SATURDAY))
        assertEquals(DayType.FRIDAY, c.dayType(DayOfWeek.FRIDAY))

        val a = FixedAppointment(c.nextAppointmentId(), "الدوام", 480, 840, setOf(DayOfWeek.SUNDAY))
        c = c.withAppointment(a)
        assertEquals("a2", c.nextAppointmentId())
        c = c.withAppointment(a.copy(end = 900))
        assertEquals(listOf(900), c.appointments.map { it.end })
        c = c.withoutAppointment(a.id)
        assertTrue(c.appointments.isEmpty())

        assertEquals(DayCustomization(workdays = c.workdays), c.reset(DayType.WORKDAY).copy(appointments = emptyList()))
    }
}
