package com.dcc.yawm.shared

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayType
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import com.dcc.yawm.shared.screen.Cities
import com.dcc.yawm.shared.screen.appMoment
import com.dcc.yawm.shared.screen.clock
import com.dcc.yawm.shared.screen.dateLine
import com.dcc.yawm.shared.screen.dayView
import com.dcc.yawm.shared.screen.daysSinceSaturday
import com.dcc.yawm.shared.screen.durShort
import com.dcc.yawm.shared.screen.durText
import com.dcc.yawm.shared.screen.nextReview
import com.dcc.yawm.shared.screen.weekView
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScreenModelTest {
    private val s = UserSettings()
    private val zone = TimeZone.of("Asia/Baghdad")
    private val oct27 = LocalDate(2026, 10, 27)

    @Test
    fun wordsAndClocks() {
        assertEquals("ساعة و3 دقائق", durText(63))
        assertEquals("ساعتين", durText(120))
        assertEquals("دقيقتين", durText(2))
        assertEquals("11 دقيقة", durText(11))
        assertEquals("12 س 30 د", durShort(750))
        assertEquals("04:22", clock(1440 + 262))
        assertEquals("الثلاثاء 27 تشرين الأول", dateLine(oct27))
    }

    @Test
    fun theAppDayRunsFromFajrToFajr() {
        val fajr28 = PrayerTimesEngine.compute(s.place, oct27.plus(1, DateTimeUnit.DAY), zone, s.prayer).wallMinutes(Prayer.FAJR)
        val night = appMoment(LocalDateTime(2026, 10, 28, 1, 30).toInstant(zone), s)
        assertEquals(oct27, night.date)
        assertEquals(1440 + 90, night.t)
        val after = appMoment(LocalDateTime(2026, 10, 28, fajr28 / 60, fajr28 % 60).toInstant(zone), s)
        assertEquals(LocalDate(2026, 10, 28), after.date)
    }

    @Test
    fun inkPointsAtTheNextPrayerAndFillsTowardIt() {
        val d = dayView(s, oct27)
        val mid = (d.times.dhuhr + d.times.asr) / 2
        val ink = d.ink(mid)
        assertEquals("العصر", ink.word)
        assertEquals(d.times.asr, ink.target)
        assertTrue(ink.progress in 0.45f..0.55f)
        val cells = d.prayers(mid)
        assertEquals(listOf(1f, 1f), cells.take(2).map { it.fill })
        assertTrue(cells[2].isNext)
        assertEquals(0f, cells[4].fill)
    }

    @Test
    fun whileAsleepTheWordIsTheWakingWord() {
        val d = dayView(s, oct27)
        val t = d.plan.bedtime + 30
        assertTrue(d.current(t)!!.isSleep)
        assertEquals(if (d.plan.qiyam) "صلاة الليل" else "الصحيان", d.ink(t).word)
        assertEquals(d.plan.wake, d.ink(t).target)
        assertEquals("النوم", d.nightInk(d.times.isha + 5).word)
    }

    @Test
    fun itemsCoverTheDayInOrderWithIcons() {
        for (date in listOf(oct27, LocalDate(2026, 10, 30), LocalDate(2026, 6, 15))) {
            val d = dayView(s, date)
            assertEquals(d.items.sortedBy { it.start }, d.items)
            assertTrue(d.items.zipWithNext().all { (a, b) -> a.end <= b.start }, "no overlap on $date")
            assertTrue(d.items.all { it.icon.isNotEmpty() })
            assertNotNull(d.current(d.times.fajr))
        }
        assertEquals(DayType.FRIDAY, dayView(s, LocalDate(2026, 10, 30)).type)
        assertEquals("mosque", dayView(s, LocalDate(2026, 10, 30)).prayers(0)[1].icon)
    }

    @Test
    fun shortWinterDaysExplainTheirChanges() {
        val hanafiWinter = s.copy(prayer = s.prayer.copy(madhab = com.dcc.yawm.shared.prayer.AsrMadhab.HANAFI))
        val d = dayView(hanafiWinter, LocalDate(2026, 12, 21))
        assertTrue(d.changes.isNotEmpty())
        assertTrue(d.changes.all { it.contains("«") })
    }

    @Test
    fun nightStepsEndAtFajr() {
        val d = dayView(s, oct27)
        val steps = d.nightSteps()
        assertEquals("isha", steps.first().icon)
        assertEquals(d.nextFajr, steps.last().start)
        assertEquals(steps.sortedBy { it.start }, steps)
    }

    @Test
    fun weekStartsOnSaturday() {
        assertEquals(0, daysSinceSaturday(LocalDate(2026, 10, 24)))
        assertEquals(3, daysSinceSaturday(oct27))
        assertEquals(LocalDate(2026, 10, 31), nextReview(oct27))
        assertEquals(LocalDate(2026, 10, 31), nextReview(LocalDate(2026, 10, 31)))
        val w = weekView(s, oct27)
        assertEquals(LocalDate(2026, 10, 24), w.saturday)
        assertEquals(7, w.days.size)
        assertTrue(w.deepWork > 0 && w.family > 0 && w.movement > 0 && w.averageSleep > 0)
        val p = w.progress(oct27, 12 * 60)
        assertTrue(p > 3f / 7 && p < 4f / 7)
    }

    @Test
    fun everyCityWorks() {
        for (c in Cities) {
            val d = dayView(s.copy(cityName = c.name, place = c.place, zoneId = c.zoneId, prayer = c.prayer), oct27)
            assertTrue(d.items.isNotEmpty(), c.name)
        }
    }
}
