package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayResolver
import com.dcc.yawm.shared.day.ResolvedDay
import com.dcc.yawm.shared.day.Slot
import com.dcc.yawm.shared.day.isFreeKey
import com.dcc.yawm.shared.day.placeTasks
import com.dcc.yawm.shared.day.tasksOn
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/*
 * Every notification of the next 48 hours, after one set of rules (owner, 2026-10-29), so many
 * alerts and tasks never turn into noise:
 *  1. Things due in the same minute become one notification, titled by the most important.
 *  2. A sound at most once in two minutes; the next one shows without sound.
 *  3. While asleep (bedtime to waking) only alarms ring.
 *  4. During focus only alarms and prayers ring.
 * Alarms (the user's own and the wake alarm) are never silenced.
 */

/** Higher rank wins the title of a merged notification. */
enum class ReminderKind(val rank: Int, val isAlarm: Boolean = false) {
    ALARM(5, true), WAKE(5, true), ADHAN(4), PRAYER(4), START(3), TASK(3), END(2), BEFORE(2), SUMMARY(1), REVIEW(1),
}

/** One thing that is due; a notification carries one or more. */
data class Line(val kind: ReminderKind, val key: String, val title: String, val body: String)

/** One notification to post. [id] is stable for the same notification, so rescheduling replaces it. */
data class Reminder(
    val kind: ReminderKind,
    val at: Instant,
    val key: String,
    val title: String,
    val body: String,
    val lines: List<Line>,
    /** Shown without sound; [why] says which rule. */
    val silent: Boolean = false,
    val why: String? = null,
    val prayer: Prayer? = null,
    /** For an item: when it ends. */
    val until: Instant? = null,
) {
    val id: Int get() = "$kind|$key|${at.epochSeconds / 60}".hashCode()
}

/** The five prayers that get an alert (sunrise is a time, not a prayer). */
val AdhanPrayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

/** How far ahead alarms are set (CLAUDE.md notifications: 48 h). */
const val SCHEDULE_HOURS = 48

/** Two sounds closer than this: the second is silent. */
const val SOUND_GAP_MINUTES = 2

private class Due(val line: Line, val at: Instant, val prayer: Prayer? = null, val until: Instant? = null)

fun upcomingReminders(settings: UserSettings, now: Instant, hours: Int = SCHEDULE_HOURS): List<Reminder> {
    val zone = runCatching { TimeZone.of(settings.zoneId) }.getOrElse { TimeZone.of(UserSettings().zoneId) }
    val end = now + hours.hours
    val nowLocal = now.toLocalDateTime(zone)
    val today = nowLocal.date
    val cache = HashMap<LocalDate, DayPrayerTimes>()
    fun times(d: LocalDate) = cache.getOrPut(d) { PrayerTimesEngine.compute(settings.place, d, zone, settings.prayer) }
    fun instant(d: LocalDate, minutes: Int): Instant = toLocalDateTime(d, minutes).toInstant(zone)
    val prefs = settings.alerts
    val due = mutableListOf<Due>()
    val asleep = mutableListOf<OpenEndRange<Instant>>()

    // From yesterday (its wake alarm can fall after now) to the day after tomorrow.
    for (offset in -1..2) {
        val d = today.plus(offset, DateTimeUnit.DAY)
        val t = times(d)
        val day = DayResolver.resolve(
            times(d.minus(1, DateTimeUnit.DAY)), t, times(d.plus(1, DateTimeUnit.DAY)),
            userBedtime = settings.bedtime, qiyamEnabled = settings.qiyamEnabled, custom = settings.day, myDay = settings.myDay,
        )
        val wakeAt = instant(d, day.plan.wake)
        asleep += instant(d, day.plan.bedtime)..<wakeAt

        for (p in AdhanPrayers) {
            val choice = prefs.prayerAlert(p)
            if (choice == PrayerAlert.NONE) continue
            val name = if (p == Prayer.DHUHR && day.blocks.any { it.key == "jumua" }) "الجمعة" else prayerName(p)
            val kind = if (choice == PrayerAlert.ADHAN) ReminderKind.ADHAN else ReminderKind.PRAYER
            due += Due(Line(kind, p.name, "وقت صلاة $name", "الساعة ${clockText(t[p], zone)}"), t[p], prayer = p)
        }
        if (settings.wakeAlarm) {
            val title = if (day.plan.qiyam) "وقت الصحيان لصلاة الليل" else "وقت الصحيان قبل الفجر"
            due += Due(Line(ReminderKind.WAKE, "wake", title, "الفجر ${clockText(times(d.plus(1, DateTimeUnit.DAY))[Prayer.FAJR], zone)}"), wakeAt)
        }
        for (a in prefs.alarms) if (a.on && d.dayOfWeek in a.days) {
            due += Due(Line(ReminderKind.ALARM, "alarm:${a.id}", a.label.ifBlank { "منبّه" }, "الساعة ${clockText(instant(d, a.minute), zone)}"), instant(d, a.minute))
        }
        if (settings.blockReminders) itemLines(day, d, settings, ::instant, zone, due)
        taskLines(day, d, today, if (d == today) nowLocal.hour * 60 + nowLocal.minute else 0, settings, ::instant, zone, due)
        if (prefs.morningSummary) {
            val n = tasksOn(d, today, settings.tasks).count { !it.done }
            val first = day.blocks.firstOrNull { it.start >= day.times.sunrise && !it.isPrayer && !isFreeKey(it.key) }
            val body = (if (n > 0) "اليوم عندك $n مهام." else "اليوم بلا مهام.") + (first?.let { " أول شغلة: ${it.title}." } ?: "")
            due += Due(Line(ReminderKind.SUMMARY, "summary", "يومك", body), instant(d, day.times.sunrise))
        }
        if (prefs.eveningReview) {
            val mine = tasksOn(d, today, settings.tasks)
            val body = "خلصت ${mine.count { it.done }} من ${mine.size} مهام. تنام ${clockText(instant(d, day.plan.bedtime), zone)} وتصحى ${clockText(wakeAt, zone)}."
            due += Due(Line(ReminderKind.REVIEW, "review", "محاسبة الليل", body), instant(d, day.plan.bedtime - 30))
        }
    }

    val focusUntil = settings.focusUntil?.let { Instant.fromEpochMilliseconds(it) }
    val merged = due.groupBy { it.at.epochSeconds / 60 }.entries.sortedBy { it.key }.map { (_, group) ->
        val sorted = group.sortedByDescending { it.line.kind.rank }
        val top = sorted.first()
        Reminder(
            kind = top.line.kind, at = top.at, key = top.line.key, title = top.line.title,
            body = (listOf(top.line.body) + sorted.drop(1).map { it.line.title }).filter { it.isNotBlank() }.joinToString("\n"),
            lines = sorted.map { it.line }, prayer = sorted.firstNotNullOfOrNull { it.prayer }, until = top.until,
        )
    }
    var lastSound: Instant? = null
    val ruled = merged.map { r ->
        val why = when {
            r.kind.isAlarm -> null
            asleep.any { r.at in it } -> "وقت نومك"
            focusUntil != null && r.at < focusUntil && r.kind != ReminderKind.ADHAN && r.kind != ReminderKind.PRAYER -> "التركيز شغّال"
            lastSound != null && r.at - lastSound!! < SOUND_GAP_MINUTES.minutes -> "رن شي قبل أقل من دقيقتين"
            else -> null
        }
        if (why == null) lastSound = r.at
        if (why == null) r else r.copy(silent = true, why = why)
    }
    return ruled.filter { it.at > now && it.at <= end }.distinctBy { it.id }.sortedBy { it.at }
}

private fun itemLines(day: ResolvedDay, d: LocalDate, s: UserSettings, instant: (LocalDate, Int) -> Instant, zone: TimeZone, due: MutableList<Due>) {
    val prefs = s.alerts
    for (b in day.blocks) {
        if (b.isPrayer || isFreeKey(b.key)) continue
        // The wake alarm already rings when the night prayer starts.
        if (b.key == "qiyam" && s.wakeAlarm) continue
        val start = instant(d, b.start)
        val stop = instant(d, b.end)
        when (val a = prefs.itemAlert(b.key)) {
            ItemAlert.START -> due += Due(Line(ReminderKind.START, b.key, "هسه: ${b.title}", "لحد ${clockText(stop, zone)}"), start, until = stop)
            ItemAlert.BEFORE5, ItemAlert.BEFORE10 -> {
                val m = if (a == ItemAlert.BEFORE5) 5 else 10
                due += Due(Line(ReminderKind.BEFORE, b.key, "بعد $m دقايق: ${b.title}", "تبدي ${clockText(start, zone)}"), instant(d, b.start - m), until = stop)
            }
            ItemAlert.OFF -> {}
        }
        if (prefs.endAlert && b.key != "qiyam") due += Due(Line(ReminderKind.END, b.key, "خلص: ${b.title}", ""), stop)
    }
}

private fun taskLines(day: ResolvedDay, d: LocalDate, today: LocalDate, from: Int, s: UserSettings, instant: (LocalDate, Int) -> Instant, zone: TimeZone, due: MutableList<Due>) {
    val mine = tasksOn(d, today, s.tasks)
    if (mine.isEmpty()) return
    val all = day.blocks.map { Slot(it.key, it.start, it.end) }
    val placed = placeTasks(mine, all.filter { isFreeKey(it.key) }, from, all)
    for (p in placed) {
        val at = p.start ?: continue
        if (p.task.done) continue
        due += Due(Line(ReminderKind.TASK, "task:${p.task.id}", "مهمة: ${p.task.title}", "${p.task.minutes} دقيقة · الساعة ${clockText(instant(d, at), zone)}"), instant(d, at))
    }
}
