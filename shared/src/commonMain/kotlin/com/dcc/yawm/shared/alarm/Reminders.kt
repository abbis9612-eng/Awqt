package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayResolver
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.day.daySpans
import com.dcc.yawm.shared.day.partOf
import com.dcc.yawm.shared.day.thingsOn
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
 * alerts and tasks never turn into noise. Since «يومك بين الصلوات» (2026-11-02) the things of a
 * part come together in one notification when the part opens, and a thing with its own clock
 * time rings at that time.
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
        for (a in prefs.alarms) if (a.ringsOn(d)) {
            due += Due(Line(ReminderKind.ALARM, "alarm:${a.id}", a.label.ifBlank { "منبّه" }, "الساعة ${clockText(instant(d, a.minute), zone)}"), instant(d, a.minute))
        }
        val spans = daySpans(t, day.plan.bedtime)
        val mine = thingsOn(d, today, settings.things, settings.workDays)
        val nowMin = if (d == today) nowLocal.hour * 60 + nowLocal.minute else null
        if (settings.blockReminders) thingLines(mine, spans, d, nowMin, ::instant, zone, due)
        if (prefs.morningSummary) {
            val open = mine.filter { !it.doneOn(d) }
            val body = if (open.isEmpty()) "اليوم بلا شغلات." else "اليوم عندك ${open.size} شغلات: " + open.take(3).joinToString("، ") { it.title } + (if (open.size > 3) "…" else ".")
            due += Due(Line(ReminderKind.SUMMARY, "summary", "يومك", body), instant(d, day.times.sunrise))
        }
        if (prefs.eveningReview) {
            val body = "خلصت ${mine.count { it.doneOn(d) }} من ${mine.size} شغلات. تنام ${clockText(instant(d, day.plan.bedtime), zone)} وتصحى ${clockText(wakeAt, zone)}."
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

/**
 * One line per part that has open things with «بأول القسم», at the part's start, carrying their
 * names; a thing with «بوكتها» and a clock time rings at that time. A thing for any time today
 * goes with the part of now.
 */
private fun thingLines(things: List<Thing>, spans: List<PartSpan>, d: LocalDate, now: Int?, instant: (LocalDate, Int) -> Instant, zone: TimeZone, due: MutableList<Due>) {
    val open = things.filter { !it.doneOn(d) }
    for (span in spans) {
        val here = open.filter { it.remind == Remind.START && partOf(it, spans, now) == span.part }
        if (here.isEmpty()) continue
        due += Due(Line(ReminderKind.START, "part:${span.part.name}", span.part.label, here.joinToString(" · ") { it.title }), instant(d, span.start), until = instant(d, span.end))
    }
    for (x in open) {
        val at = x.at ?: continue
        if (x.remind != Remind.AT) continue
        due += Due(Line(ReminderKind.TASK, "thing:${x.id}", x.title, "الساعة ${clockText(instant(d, at), zone)}"), instant(d, at))
    }
}
