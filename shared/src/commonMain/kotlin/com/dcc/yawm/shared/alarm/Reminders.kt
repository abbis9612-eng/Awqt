package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayResolver
import com.dcc.yawm.shared.day.toLocalDateTime
import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

enum class ReminderKind { ADHAN, BLOCK, WAKE }

/** One alarm to set. [id] is stable for the same reminder, so rescheduling replaces it. */
data class Reminder(
    val kind: ReminderKind,
    val at: Instant,
    val key: String,
    val title: String,
    val prayer: Prayer? = null,
    /** For blocks: when the block ends. */
    val until: Instant? = null,
) {
    val id: Int get() = "$kind|$key|${at.epochSeconds / 60}".hashCode()
}

/** The five prayers that get an adhan (sunrise is a time, not a prayer). */
val AdhanPrayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

/** How far ahead alarms are set (CLAUDE.md notifications: 48 h). */
const val SCHEDULE_HOURS = 48

/**
 * Everything to ring from [now] (exclusive) to [now] + [SCHEDULE_HOURS], sorted by time. Prayer
 * blocks get no block reminder (the adhan covers them), nor does the qiyam block when the wake
 * alarm rings at the same moment.
 */
fun upcomingReminders(settings: UserSettings, now: Instant, hours: Int = SCHEDULE_HOURS): List<Reminder> {
    val zone = runCatching { TimeZone.of(settings.zoneId) }.getOrElse { TimeZone.of(UserSettings().zoneId) }
    val end = now + hours.hours
    val today = now.toLocalDateTime(zone).date
    val cache = HashMap<LocalDate, DayPrayerTimes>()
    fun times(d: LocalDate) = cache.getOrPut(d) { PrayerTimesEngine.compute(settings.place, d, zone, settings.prayer) }
    fun instant(d: LocalDate, minutes: Int): Instant = toLocalDateTime(d, minutes).toInstant(zone)

    val out = mutableListOf<Reminder>()
    // From yesterday (its wake alarm can fall after now) to the day after tomorrow.
    for (offset in -1..2) {
        val d = today.plus(offset, DateTimeUnit.DAY)
        val t = times(d)
        for (p in AdhanPrayers) out += Reminder(ReminderKind.ADHAN, t[p], p.name, p.name, prayer = p)
        val day = DayResolver.resolve(
            times(d.minus(1, DateTimeUnit.DAY)), t, times(d.plus(1, DateTimeUnit.DAY)),
            userBedtime = settings.bedtime, qiyamEnabled = settings.qiyamEnabled, custom = settings.day,
        )
        if (settings.wakeAlarm) {
            out += Reminder(ReminderKind.WAKE, instant(d, day.plan.wake), "wake", if (day.plan.qiyam) "qiyam" else "fajr")
        }
        if (settings.blockReminders) {
            for (b in day.blocks) {
                if (b.isPrayer || (b.key == "qiyam" && settings.wakeAlarm)) continue
                out += Reminder(ReminderKind.BLOCK, instant(d, b.start), b.key, b.title, until = instant(d, b.end))
            }
        }
    }
    return out.filter { it.at > now && it.at <= end }.distinctBy { it.id }.sortedBy { it.at }
}
