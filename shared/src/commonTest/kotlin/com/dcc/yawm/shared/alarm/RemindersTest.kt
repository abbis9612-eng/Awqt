package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayResolver
import com.dcc.yawm.shared.day.toLocalDateTime
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.PrayerSettings
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class RemindersTest {
    private val sulaymaniyah = UserSettings()
    private val zone = TimeZone.of("Asia/Baghdad")
    private val now = LocalDateTime(2026, 10, 27, 9, 0).toInstant(zone)

    @Test
    fun windowIsFortyEightHoursSortedAndUnique() {
        val r = upcomingReminders(sulaymaniyah, now)
        assertTrue(r.isNotEmpty())
        assertTrue(r.all { it.at > now && it.at <= now + 48.hours })
        assertEquals(r.sortedBy { it.at }, r)
        assertEquals(r.size, r.map { it.id }.toSet().size)
    }

    @Test
    fun adhanRingsExactlyAtEachPrayerTime() {
        val adhans = upcomingReminders(sulaymaniyah, now).filter { it.kind == ReminderKind.ADHAN }
        // 09:00 on the 27th to 09:00 on the 29th: Dhuhr..Isha twice, and Fajr on the 28th and 29th.
        assertEquals(10, adhans.size)
        val expected = (0..2).flatMap { off ->
            val d = LocalDate(2026, 10, 27).plus(off, DateTimeUnit.DAY)
            val t = PrayerTimesEngine.compute(sulaymaniyah.place, d, zone, sulaymaniyah.prayer)
            AdhanPrayers.map { t[it] }
        }.filter { it > now && it <= now + 48.hours }
        assertEquals(expected, adhans.map { it.at })
    }

    @Test
    fun wakeAlarmFollowsTonightsPlan() {
        val wakes = upcomingReminders(sulaymaniyah, now).filter { it.kind == ReminderKind.WAKE }
        assertEquals(2, wakes.size)
        val d = LocalDate(2026, 10, 27)
        fun times(x: LocalDate) = PrayerTimesEngine.compute(sulaymaniyah.place, x, zone, sulaymaniyah.prayer)
        val plan = DayResolver.resolve(times(d.minus(1, DateTimeUnit.DAY)), times(d), times(d.plus(1, DateTimeUnit.DAY))).plan
        assertEquals(toLocalDateTime(d, plan.wake).toInstant(zone), wakes.first().at)
    }

    @Test
    fun noBlockReminderForPrayersOrForQiyamWhenTheWakeAlarmRings() {
        val r = upcomingReminders(sulaymaniyah, now)
        val blockKeys = r.filter { it.kind == ReminderKind.BLOCK }.map { it.key }.toSet()
        assertTrue(blockKeys.none { it in setOf("fajr", "dhuhr", "asr", "maghrib", "isha", "jumua", "qiyam") })
        assertTrue("deep1" in blockKeys)
        assertTrue(r.filter { it.kind == ReminderKind.BLOCK }.all { it.until != null && it.until > it.at })
    }

    @Test
    fun switchesAreRespected() {
        val quiet = upcomingReminders(sulaymaniyah.copy(blockReminders = false, wakeAlarm = false), now)
        assertTrue(quiet.all { it.kind == ReminderKind.ADHAN })
        val noWake = upcomingReminders(sulaymaniyah.copy(wakeAlarm = false), now)
        assertTrue(noWake.none { it.kind == ReminderKind.WAKE })
        // Oct 27 has a qiyam night in Sulaymaniyah, so its block is reminded instead.
        assertTrue(noWake.any { it.kind == ReminderKind.BLOCK && it.key == "qiyam" })
    }

    @Test
    fun londonAcrossTheClockChangeAndInSummer() {
        val london = UserSettings(
            cityName = "London", place = GeoPoint(51.5074, -0.1278), zoneId = "Europe/London",
            prayer = PrayerSettings(CalcMethod.MUSLIM_WORLD_LEAGUE, highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
        )
        val z = TimeZone.of("Europe/London")
        for (start in listOf(LocalDateTime(2026, 10, 24, 12, 0), LocalDateTime(2026, 6, 20, 12, 0), LocalDateTime(2026, 3, 28, 23, 0))) {
            val n = start.toInstant(z)
            val r = upcomingReminders(london, n)
            assertEquals(r.sortedBy { it.at }, r)
            assertEquals(10, r.count { it.kind == ReminderKind.ADHAN }, "adhans from $start")
        }
    }

    @Test
    fun wordingNamesThePrayerAndTheTime() {
        val r = upcomingReminders(sulaymaniyah, now)
        val dhuhr = r.first { it.kind == ReminderKind.ADHAN }
        val text = reminderText(dhuhr, zone)
        assertEquals("حان وقت صلاة ${prayerName(dhuhr.prayer!!)}", text.title)
        assertEquals("الساعة ${clockText(dhuhr.at, zone)}", text.body)
        val block = r.first { it.kind == ReminderKind.BLOCK }
        assertEquals(block.title, reminderText(block, zone).title)
        assertTrue(reminderText(block, zone).body.startsWith("من "))
        assertEquals(1, batteryAdvice("unknown-maker").size)
        assertTrue(batteryAdvice("Xiaomi").size > 1)
    }
}
