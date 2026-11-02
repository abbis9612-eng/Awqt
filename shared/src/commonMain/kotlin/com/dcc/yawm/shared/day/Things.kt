package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/*
 * «يومك بين الصلوات» (prototype v11, owner's approval 2026-11-02): the day is six parts between
 * the prayers, and everything the user wants to do is one kind of thing («شغلة») that sits in a
 * part, or at a clock time, today, tomorrow, every day or on working days. The prayer times and
 * the night plan come from the same engine and resolver as before; only what is shown changes.
 */

/** The six parts of the day, each opened by a prayer (the morning by sunrise). */
enum class DayPart(val label: String, val opens: Prayer, val endLabel: String) {
    FAJR("بعد الفجر", Prayer.FAJR, "الشروق"),
    MORNING("الصبح", Prayer.SUNRISE, "الظهر"),
    DHUHR("بعد الظهر", Prayer.DHUHR, "العصر"),
    ASR("بعد العصر", Prayer.ASR, "المغرب"),
    MAGHRIB("بعد المغرب", Prayer.MAGHRIB, "العشاء"),
    ISHA("بعد العشاء", Prayer.ISHA, "النوم"),
}

/** A part of one day, in wall-clock minutes from that day's midnight (above 1440 after midnight). */
data class PartSpan(val part: DayPart, val start: Int, val end: Int)

/** The six parts of a day; the last one ends at [bedtime] (never before Isha + 30, as the night plan). */
fun daySpans(times: DayPrayerTimes, bedtime: Int): List<PartSpan> {
    val starts = DayPart.entries.map { times.wallMinutes(it.opens) }
    val isha = starts.last()
    val ends = starts.drop(1) + maxOf(bedtime, isha + ISHA_TO_BED_MIN)
    return DayPart.entries.mapIndexed { i, p -> PartSpan(p, starts[i], ends[i]) }
}

/** The part a minute falls in; null before Fajr or after bedtime (the night). */
fun partAt(minute: Int, spans: List<PartSpan>): PartSpan? = spans.firstOrNull { minute >= it.start && minute < it.end }

enum class Repeat(val label: String) { ONCE("مرة وحدة"), DAILY("كل يوم"), WORKDAYS("أيام الدوام") }

/** When a thing tells the user: never, with its part's notification, or at its own clock time. */
enum class Remind { NONE, START, AT }

data class Thing(
    val id: String,
    val title: String,
    /** The part it belongs to; null = the part of its clock time, or «any time» on its day. */
    val part: DayPart? = null,
    /** For [Repeat.ONCE], the day; null = «بعدين» (no day yet). Ignored when it repeats. */
    val date: LocalDate? = null,
    /** A fixed clock time, minutes from midnight. */
    val at: Int? = null,
    val end: Int? = null,
    val repeat: Repeat = Repeat.ONCE,
    val remind: Remind = Remind.START,
    /** The days it was marked done (one day for a one-off). */
    val done: Set<LocalDate> = emptySet(),
) {
    val isLater: Boolean get() = repeat == Repeat.ONCE && date == null
    fun doneOn(d: LocalDate): Boolean = d in done || (repeat == Repeat.ONCE && done.isNotEmpty())
    fun toggled(d: LocalDate): Thing = if (doneOn(d)) copy(done = if (repeat == Repeat.ONCE) emptySet() else done - d) else copy(done = keepRecent(done + d, d))
}

/** Keep the done-log of a repeating thing short: the last four weeks are enough for the week view. */
private fun keepRecent(s: Set<LocalDate>, d: LocalDate): Set<LocalDate> = s.filter { it.toEpochDays() > d.toEpochDays() - 28 }.toSet()

/** The things of [date]: repeating ones that fall on it, its one-offs, and older open one-offs carried to today. */
fun thingsOn(date: LocalDate, today: LocalDate, things: List<Thing>, workDays: Set<DayOfWeek>): List<Thing> = things.filter { t ->
    when (t.repeat) {
        Repeat.DAILY -> true
        Repeat.WORKDAYS -> date.dayOfWeek in workDays
        Repeat.ONCE -> {
            val d = t.date ?: return@filter false
            d == date || (date == today && d < today && !t.doneOn(today))
        }
    }
}

/** Where a thing shows on [date]: its part, the part of its clock time, or (any time) the part of [now]. */
fun partOf(t: Thing, spans: List<PartSpan>, now: Int?): DayPart? = t.part
    ?: t.at?.let { at -> partAt(at, spans)?.part ?: if (at < spans.first().start) DayPart.FAJR else DayPart.ISHA }
    ?: now?.let { m -> partAt(m, spans)?.part ?: if (m < spans.first().start) DayPart.FAJR else null }
    ?: DayPart.MORNING

fun nextThingId(things: List<Thing>): String = "h${(things.mapNotNull { it.id.removePrefix("h").toIntOrNull() }.maxOrNull() ?: 0) + 1}"

/**
 * The first time the app opens after the update: the user's tasks and the day they built become
 * things, so nothing they entered is lost. Work hours become «الدوام» on working days.
 */
fun migrateToThings(tasks: List<TaskItem>, myDay: MyDayPlan?): List<Thing> = buildList {
    if (myDay != null && myDay.hasWork) add(Thing("h1", myDay.life.workTitle!!, at = myDay.workStart, end = myDay.workEnd, repeat = Repeat.WORKDAYS, remind = Remind.NONE))
    for (i in myDay?.items.orEmpty()) {
        val part = when (i.anchor) {
            ItemAnchor.FAJR -> DayPart.FAJR
            ItemAnchor.DHUHR -> DayPart.DHUHR
            ItemAnchor.ASR -> DayPart.ASR
            ItemAnchor.MAGHRIB -> DayPart.MAGHRIB
            ItemAnchor.ISHA -> DayPart.ISHA
            ItemAnchor.FIXED -> null
        }
        add(Thing("h${size + 1}", i.title, part, at = if (i.anchor == ItemAnchor.FIXED) i.at else null, repeat = Repeat.DAILY))
    }
    for (t in tasks) add(Thing("h${size + 1}", t.title, null, t.day, at = t.pinAt, remind = if (t.pinAt != null) Remind.AT else Remind.START, done = setOfNotNull(t.doneOn)))
}
