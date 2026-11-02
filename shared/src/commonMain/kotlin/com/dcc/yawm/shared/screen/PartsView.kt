package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.data.UserSettings
import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.DayType
import com.dcc.yawm.shared.day.PartSpan
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import com.dcc.yawm.shared.day.daySpans
import com.dcc.yawm.shared.day.partOf
import com.dcc.yawm.shared.day.thingsOn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/*
 * The one page of «يومك بين الصلوات» (prototype v11): the part you are in, the parts to come,
 * the night with its alarms, what has passed (folded), tomorrow and «بعدين». Computed here, never
 * in the UI, from the same prayer times and night plan as before.
 */

data class PartOfDay(val span: PartSpan, val things: List<Thing>) {
    val part: DayPart get() = span.part
}

class PartsView(
    val date: LocalDate,
    val day: DayView,
    val parts: List<PartOfDay>,
    val tomorrow: List<Thing>,
    val later: List<Thing>,
    /** The user's alarms that ring on this date's night or the next morning, in clock order. */
    val alarms: List<UserAlarm>,
) {
    val bedtime: Int get() = day.plan.bedtime
    val wake: Int get() = day.plan.wake

    /** The part of [t], or null at night (after bedtime, or before Fajr). */
    fun current(t: Int): PartOfDay? = parts.firstOrNull { t >= it.span.start && t < it.span.end }
    fun asleep(t: Int): Boolean = current(t) == null
    fun past(t: Int): List<PartOfDay> = parts.filter { it.span.end <= t }
    fun future(t: Int): List<PartOfDay> = parts.filter { it.span.start > t }

    /** The key the prayer of a part is marked done with (the day log of the earlier screens). */
    fun prayerKey(part: DayPart): String? = when (part) {
        DayPart.FAJR -> "fajr"
        DayPart.MORNING -> null
        DayPart.DHUHR -> if (day.type == DayType.FRIDAY) "jumua" else "dhuhr"
        DayPart.ASR -> "asr"
        DayPart.MAGHRIB -> "maghrib"
        DayPart.ISHA -> "isha"
    }

    fun prayerName(part: DayPart): String? = when (part) {
        DayPart.MORNING -> null
        DayPart.DHUHR -> if (day.type == DayType.FRIDAY) "الجمعة" else "الظهر"
        else -> com.dcc.yawm.shared.alarm.prayerName(part.opens)
    }
}

/** [t] is the app minute when [date] is today (it may pass 1440 before Fajr), else null. */
fun partsView(settings: UserSettings, date: LocalDate, today: LocalDate, t: Int?): PartsView {
    val day = dayView(settings, date)
    val spans = daySpans(day.times, day.plan.bedtime)
    val mine = thingsOn(date, today, settings.things, settings.workDays)
    val now = if (date == today) t else null
    val byPart = mine.groupBy { partOf(it, spans, now) }
    val parts = spans.map { s -> PartOfDay(s, byPart[s.part].orEmpty().sortedWith(compareBy({ it.at ?: Int.MAX_VALUE }, { it.title }))) }
    val next = date.plus(1, DateTimeUnit.DAY)
    val tomorrow = settings.things.filter { it.repeat == Repeat.ONCE && it.date == next && date == today }
    val later = settings.things.filter { it.isLater }
    val alarms = settings.alerts.alarms.filter { it.onceOn == null || it.onceOn == date || it.onceOn == next }.sortedBy { it.minute }
    return PartsView(date, day, parts, tomorrow, later, alarms)
}

/** The part a new thing goes to when the words name none: the part of now (after bedtime, the last one). */
fun partNow(view: PartsView, t: Int?): DayPart = t?.let { view.current(it)?.part } ?: if (t != null && t >= view.bedtime) DayPart.ISHA else DayPart.FAJR

