package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.data.decodeAlerts
import com.dcc.yawm.shared.data.encodeAlerts
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
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
    private val today = LocalDate(2026, 10, 27)
    private val things = listOf(
        Thing("h1", "رياضة", DayPart.ASR, repeat = Repeat.DAILY),
        Thing("h2", "قرآن", DayPart.FAJR, repeat = Repeat.DAILY),
        Thing("h3", "زيارة", date = today, at = 17 * 60 + 40, remind = Remind.AT),
        Thing("h4", "الدوام", at = 8 * 60, end = 14 * 60, repeat = Repeat.WORKDAYS, remind = Remind.NONE),
        Thing("h5", "مشي", DayPart.MAGHRIB, repeat = Repeat.DAILY, remind = Remind.NONE),
    )
    private val withThings = sulaymaniyah.copy(things = things)
    private fun times(x: LocalDate) = PrayerTimesEngine.compute(sulaymaniyah.place, x, zone, sulaymaniyah.prayer)

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
        val plan = DayResolver.resolve(times(today.minus(1, DateTimeUnit.DAY)), times(today), times(today.plus(1, DateTimeUnit.DAY))).plan
        assertEquals(toLocalDateTime(today, plan.wake).toInstant(zone), wakes.first().at)
    }

    @Test
    fun eachPartOpensWithOneNotificationOfItsThings() {
        val r = upcomingReminders(withThings, now)
        val starts = r.flatMap { n -> n.lines.filter { it.kind == ReminderKind.START }.map { n to it } }
        assertTrue(starts.all { (_, l) -> l.key.startsWith("part:") })
        // «رياضة» after Asr on both days, at the Asr minute, in the same notification as the Asr alert.
        val asr = starts.filter { (_, l) -> l.key == "part:ASR" }
        assertEquals(2, asr.size)
        for ((n, l) in asr) {
            assertEquals("بعد العصر", l.title)
            assertTrue("رياضة" in l.body)
            assertEquals(ReminderKind.PRAYER, n.kind, "the prayer titles the shared notification")
        }
        assertEquals(times(today)[Prayer.ASR], asr.first().first.at)
        // A thing with «لا» is never announced; «بعد المغرب» holds only «مشي» (no reminder), so no line.
        assertTrue(starts.none { (_, l) -> l.key == "part:MAGHRIB" })
        assertTrue(r.flatMap { it.lines }.filter { it.kind == ReminderKind.START || it.kind == ReminderKind.TASK }.none { "مشي" in it.body || "الدوام" in it.body })
    }

    @Test
    fun switchesAreRespected() {
        val quiet = upcomingReminders(
            withThings.copy(blockReminders = false, wakeAlarm = false, alerts = AlertPrefs(morningSummary = false, eveningReview = false)), now,
        )
        assertTrue(quiet.all { it.kind == ReminderKind.PRAYER }, quiet.map { it.kind }.toString())
        val noWake = upcomingReminders(withThings.copy(wakeAlarm = false), now)
        assertTrue(noWake.none { it.kind == ReminderKind.WAKE })
        val none = upcomingReminders(sulaymaniyah.copy(alerts = AlertPrefs(prayer = AdhanPrayers.associateWith { PrayerAlert.NONE })), now)
        assertTrue(none.flatMap { it.lines }.none { it.kind == ReminderKind.PRAYER || it.kind == ReminderKind.ADHAN })
        val adhan = upcomingReminders(sulaymaniyah.copy(alerts = AlertPrefs(prayer = mapOf(Prayer.MAGHRIB to PrayerAlert.ADHAN))), now)
        assertTrue(adhan.flatMap { it.lines }.filter { it.kind == ReminderKind.ADHAN }.all { it.key == "MAGHRIB" })
        assertEquals(2, adhan.flatMap { it.lines }.count { it.kind == ReminderKind.ADHAN })
    }

    @Test
    fun aThingWithItsOwnTimeRingsThenAndDoneThingsAreQuiet() {
        val lines = upcomingReminders(withThings, now).flatMap { it.lines }
        val visit = lines.single { it.key == "thing:h3" }
        assertEquals(ReminderKind.TASK, visit.kind)
        assertTrue("17:40" in visit.body)
        // Done today: not in today's Asr line, still there tomorrow (it repeats).
        val doneToday = withThings.copy(things = things.map { if (it.id == "h1") it.toggled(today) else it })
        val asr = upcomingReminders(doneToday, now).flatMap { it.lines }.filter { it.key == "part:ASR" }
        assertEquals(1, asr.size)
    }

    @Test
    fun alarmsRingDailyOnWorkdaysOrOnce() {
        val daily = newAlarm("a1", "صحيان", 5 * 60 + 45, Repeat.DAILY, withThings.workDays, today, 9 * 60)
        val once = newAlarm("a2", "دواء", 5 * 60 + 45, Repeat.ONCE, withThings.workDays, today, 9 * 60)
        assertEquals(today.plus(1, DateTimeUnit.DAY), once.onceOn, "05:45 has passed at 09:00, so it rings tomorrow")
        fun count(a: UserAlarm) = upcomingReminders(sulaymaniyah.copy(alerts = AlertPrefs(alarms = listOf(a))), now)
            .flatMap { it.lines }.count { it.kind == ReminderKind.ALARM }
        assertEquals(2, count(daily))
        assertEquals(1, count(once))
        assertEquals(0, count(daily.copy(on = false)))
        val friday = UserAlarm("a3", "جمعة", 6 * 60, setOf(DayOfWeek.FRIDAY))
        assertEquals(0, count(friday), "27-29 October 2026 are Tuesday to Thursday")
    }

    @Test
    fun theRulesKeepThemInOrder() {
        val alarm = UserAlarm("a1", "الدواء", 13 * 60 + 45, on = true)
        val s = withThings.copy(alerts = AlertPrefs(alarms = listOf(alarm)))
        val r = upcomingReminders(s, now)
        // One notification per minute.
        assertEquals(r.size, r.map { it.at }.toSet().size)
        // A merged one is titled by its most important line.
        for (n in r) assertEquals(n.lines.maxOf { it.kind.rank }, n.kind.rank)
        assertTrue(r.any { it.lines.size > 1 }, "a prayer and its part should share a minute")
        // Alarms always ring; nothing else rings within two minutes of the last sound or while asleep.
        assertTrue(r.filter { it.kind.isAlarm }.none { it.silent })
        assertTrue(r.any { it.kind == ReminderKind.ALARM && it.title == "الدواء" })
        var last: kotlin.time.Instant? = null
        for (n in r.filter { !it.silent }) {
            if (!n.kind.isAlarm) last?.let { assertTrue(n.at - it >= 2.minutes, "${n.title} rings too soon") }
            last = n.at
        }
        val plan = DayResolver.resolve(times(today.minus(1, DateTimeUnit.DAY)), times(today), times(today.plus(1, DateTimeUnit.DAY))).plan
        val sleep = toLocalDateTime(today, plan.bedtime).toInstant(zone)..<toLocalDateTime(today, plan.wake).toInstant(zone)
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
    fun theDailySummariesCountTheThings() {
        val lines = upcomingReminders(withThings, now).flatMap { it.lines }
        val summary = lines.first { it.kind == ReminderKind.SUMMARY }
        assertTrue("شغلات" in summary.body && "رياضة" in summary.body, summary.body)
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
        val part = upcomingReminders(withThings, now).flatMap { it.lines }.first { it.kind == ReminderKind.START }
        assertTrue(part.title.startsWith("بعد ") || part.title == "الصبح", part.title)
        assertEquals(1, batteryAdvice("unknown-maker").size)
        assertTrue(batteryAdvice("Xiaomi").size > 1)
        val wake = r.first { it.kind == ReminderKind.WAKE }
        assertTrue("الصحيان" in wake.title)
    }

    @Test
    fun alertChoicesSurviveSaving() {
        val a = AlertPrefs(
            itemDefault = ItemAlert.BEFORE5, itemOverrides = mapOf("my:m1" to ItemAlert.OFF), endAlert = false, style = AlertStyle.VIBRATE,
            prayer = mapOf(Prayer.FAJR to PrayerAlert.ADHAN), alarms = listOf(UserAlarm("a1", "رياضة", 390, setOf(DayOfWeek.MONDAY), on = false), UserAlarm("a2", "دواء", 400, onceOn = LocalDate(2026, 11, 3))),
            morningSummary = false, eveningReview = true,
        )
        assertEquals(a, decodeAlerts(encodeAlerts(a)))
        assertEquals(AlertPrefs(), decodeAlerts("junk"))
    }
}
