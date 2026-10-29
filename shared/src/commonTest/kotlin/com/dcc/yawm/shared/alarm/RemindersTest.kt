package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.decodeAlerts
import com.dcc.yawm.shared.data.encodeAlerts
import com.dcc.yawm.shared.day.TaskItem
import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.DayOfWeek
import kotlin.time.Duration.Companion.minutes
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
    fun prayerAlertRingsExactlyAtEachPrayerTime() {
        val adhans = upcomingReminders(sulaymaniyah, now).flatMap { r -> r.lines.filter { it.kind == ReminderKind.PRAYER }.map { r.at } }
        // 09:00 on the 27th to 09:00 on the 29th: Dhuhr..Isha twice, and Fajr on the 28th and 29th.
        assertEquals(10, adhans.size)
        val expected = (0..2).flatMap { off ->
            val d = LocalDate(2026, 10, 27).plus(off, DateTimeUnit.DAY)
            val t = PrayerTimesEngine.compute(sulaymaniyah.place, d, zone, sulaymaniyah.prayer)
            AdhanPrayers.map { t[it] }
        }.filter { it > now && it <= now + 48.hours }
        assertEquals(expected, adhans)
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
    fun noItemReminderForPrayersOrForQiyamWhenTheWakeAlarmRings() {
        val r = upcomingReminders(sulaymaniyah, now)
        val keys = r.flatMap { it.lines }.filter { it.kind == ReminderKind.START }.map { it.key }.toSet()
        assertTrue(keys.none { it in setOf("fajr", "dhuhr", "asr", "maghrib", "isha", "jumua", "qiyam") })
        assertTrue("deep1" in keys)
        assertTrue(r.filter { it.kind == ReminderKind.START }.all { it.until != null && it.until > it.at })
    }

    @Test
    fun switchesAreRespected() {
        val quiet = upcomingReminders(
            sulaymaniyah.copy(blockReminders = false, wakeAlarm = false, alerts = AlertPrefs(morningSummary = false, eveningReview = false)), now,
        )
        assertTrue(quiet.all { it.kind == ReminderKind.PRAYER }, quiet.map { it.kind }.toString())
        val noWake = upcomingReminders(sulaymaniyah.copy(wakeAlarm = false), now)
        assertTrue(noWake.none { it.kind == ReminderKind.WAKE })
        // Oct 27 has a qiyam night in Sulaymaniyah, so its start is reminded instead.
        assertTrue(noWake.flatMap { it.lines }.any { it.kind == ReminderKind.START && it.key == "qiyam" })
        val none = upcomingReminders(sulaymaniyah.copy(alerts = AlertPrefs(prayer = AdhanPrayers.associateWith { PrayerAlert.NONE })), now)
        assertTrue(none.flatMap { it.lines }.none { it.kind == ReminderKind.PRAYER || it.kind == ReminderKind.ADHAN })
        val adhan = upcomingReminders(sulaymaniyah.copy(alerts = AlertPrefs(prayer = mapOf(Prayer.MAGHRIB to PrayerAlert.ADHAN))), now)
        assertTrue(adhan.flatMap { it.lines }.filter { it.kind == ReminderKind.ADHAN }.all { it.key == "MAGHRIB" })
        assertEquals(2, adhan.flatMap { it.lines }.count { it.kind == ReminderKind.ADHAN })
    }

    @Test
    fun beforeAndOffPerItem() {
        val s = sulaymaniyah.copy(alerts = AlertPrefs(itemOverrides = mapOf("deep1" to ItemAlert.BEFORE10, "msgs1" to ItemAlert.OFF), endAlert = false))
        val r = upcomingReminders(s, now)
        val lines = r.flatMap { it.lines }
        assertTrue(lines.none { it.key == "msgs1" })
        val before = r.first { it.lines.any { l -> l.key == "deep1" && l.kind == ReminderKind.BEFORE } }
        assertTrue(clockText(before.at + 10.minutes, zone) in before.lines.first { it.key == "deep1" }.body, before.toString())
        assertTrue(lines.none { it.kind == ReminderKind.END })
    }

    @Test
    fun theRulesKeepThemInOrder() {
        val alarm = UserAlarm("a1", "الدواء", 13 * 60 + 45, on = true)
        val s = sulaymaniyah.copy(alerts = AlertPrefs(alarms = listOf(alarm)))
        val r = upcomingReminders(s, now)
        // One notification per minute.
        assertEquals(r.size, r.map { it.at }.toSet().size)
        // A merged one is titled by its most important line.
        for (n in r) assertEquals(n.lines.maxOf { it.kind.rank }, n.kind.rank)
        assertTrue(r.any { it.lines.size > 1 }, "an end and a start should share a minute")
        // Alarms always ring; nothing else rings within two minutes of the last sound or while asleep.
        assertTrue(r.filter { it.kind.isAlarm }.none { it.silent })
        assertTrue(r.any { it.kind == ReminderKind.ALARM && it.title == "الدواء" })
        var last: kotlin.time.Instant? = null
        for (n in r.filter { !it.silent }) {
            if (!n.kind.isAlarm) last?.let { assertTrue(n.at - it >= 2.minutes, "${n.title} rings too soon") }
            last = n.at
        }
        val d = LocalDate(2026, 10, 27)
        fun times(x: LocalDate) = PrayerTimesEngine.compute(sulaymaniyah.place, x, zone, sulaymaniyah.prayer)
        val plan = DayResolver.resolve(times(d.minus(1, DateTimeUnit.DAY)), times(d), times(d.plus(1, DateTimeUnit.DAY))).plan
        val sleep = toLocalDateTime(d, plan.bedtime).toInstant(zone)..<toLocalDateTime(d, plan.wake).toInstant(zone)
        assertTrue(r.filter { it.at in sleep && !it.kind.isAlarm }.all { it.silent && it.why == "وقت نومك" })
    }

    @Test
    fun focusSilencesAllButAlarmsAndPrayers() {
        val until = now + 3.hours
        val r = upcomingReminders(sulaymaniyah.copy(focusUntil = until.toEpochMilliseconds()), now)
        val inFocus = r.filter { it.at < until }
        assertTrue(inFocus.isNotEmpty())
        for (n in inFocus) {
            if (n.kind.isAlarm || n.prayer != null && n.kind.rank == 4) continue
            assertTrue(n.silent, "${n.title} rang during focus")
        }
    }

    @Test
    fun tasksAndTheDailySummaries() {
        val today = LocalDate(2026, 10, 27)
        val s = sulaymaniyah.copy(tasks = listOf(TaskItem("t1", "اتصل بالمحاسب", 15, today), TaskItem("t2", "زيارة", 30, today, pinAt = 17 * 60 + 40)))
        val lines = upcomingReminders(s, now).flatMap { it.lines }
        assertTrue(lines.any { it.kind == ReminderKind.TASK && it.title == "مهمة: اتصل بالمحاسب" })
        assertTrue(lines.any { it.kind == ReminderKind.TASK && it.title == "مهمة: زيارة" && "17:40" in it.body })
        assertTrue(lines.any { it.kind == ReminderKind.SUMMARY })
        assertTrue(lines.any { it.kind == ReminderKind.REVIEW && "تنام" in it.body })
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
            assertEquals(10, r.flatMap { it.lines }.count { it.kind == ReminderKind.PRAYER }, "prayer alerts from $start")
        }
    }

    @Test
    fun wordingNamesThePrayerAndTheTime() {
        val r = upcomingReminders(sulaymaniyah, now)
        val dhuhr = r.flatMap { it.lines }.first { it.kind == ReminderKind.PRAYER }
        assertTrue(dhuhr.title.startsWith("وقت صلاة "))
        assertTrue(dhuhr.body.startsWith("الساعة "))
        val start = r.first { it.kind == ReminderKind.START }
        assertTrue(start.title.startsWith("هسه: "))
        assertTrue(start.body.startsWith("لحد "))
        assertEquals(1, batteryAdvice("unknown-maker").size)
        assertTrue(batteryAdvice("Xiaomi").size > 1)
        val wake = r.first { it.kind == ReminderKind.WAKE }
        assertTrue("الصحيان" in wake.title)
    }

    @Test
    fun alertChoicesSurviveSaving() {
        val a = AlertPrefs(
            itemDefault = ItemAlert.BEFORE5, itemOverrides = mapOf("my:m1" to ItemAlert.OFF), endAlert = false, style = AlertStyle.VIBRATE,
            prayer = mapOf(Prayer.FAJR to PrayerAlert.ADHAN), alarms = listOf(UserAlarm("a1", "رياضة", 390, setOf(DayOfWeek.MONDAY), on = false)),
            morningSummary = false, eveningReview = true,
        )
        assertEquals(a, decodeAlerts(encodeAlerts(a)))
        assertEquals(AlertPrefs(), decodeAlerts("junk"))
    }
}
