package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.prayer.DayPrayerTimes
import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

/*
 * Segment fitting and day resolution, ported line by line from fit_segment() and resolve_day() in
 * docs/reference/resolver_reference.py (CLAUDE.md rule 7). Times are wall-clock minutes from the
 * day's local midnight, exactly as in the reference; use [toLocalDateTime] at the edges.
 */

/** The six times of a day as wall-clock minutes from that day's local midnight. */
data class DayMinutes(val fajr: Int, val sunrise: Int, val dhuhr: Int, val asr: Int, val maghrib: Int, val isha: Int) {
    companion object {
        fun of(t: DayPrayerTimes) = DayMinutes(
            t.wallMinutes(Prayer.FAJR), t.wallMinutes(Prayer.SUNRISE), t.wallMinutes(Prayer.DHUHR),
            t.wallMinutes(Prayer.ASR), t.wallMinutes(Prayer.MAGHRIB), t.wallMinutes(Prayer.ISHA),
        )
    }
}

enum class AdjustmentKind { SHRUNK, DROPPED, OVERFLOW }

/** Every change the fitting made, kept so the UI can explain it (CLAUDE.md, day template). */
data class Adjustment(val kind: AdjustmentKind, val key: String?) {
    /** Same text as the reference's notes, e.g. "shrunk:prep". */
    override fun toString() = when (kind) {
        AdjustmentKind.SHRUNK -> "shrunk:$key"
        AdjustmentKind.DROPPED -> "dropped:$key"
        AdjustmentKind.OVERFLOW -> "overflow"
    }
}

data class ResolvedBlock(
    val key: String,
    val title: String,
    val role: Role,
    val start: Int,
    val end: Int,
    val isDeepWork: Boolean,
    val isPrayer: Boolean,
    /** The preferred length, when the block was shrunk below it. */
    val shrunkFromMinutes: Int? = null,
) {
    val minutes: Int get() = end - start
}

data class FittedSegment(val start: Int, val end: Int, val blocks: List<ResolvedBlock>, val adjustments: List<Adjustment>)

fun fitSegment(start: Int, end: Int, specs: List<BlockSpec>): FittedSegment {
    val adjustments = mutableListOf<Adjustment>()
    val avail = end - start
    val dur = specs.associate { it.key to if (it.flex) it.minMinutes else it.preferredMinutes }.toMutableMap()
    val live = specs.toMutableList()
    fun total() = live.sumOf { dur.getValue(it.key) }
    fun shrink(pool: List<BlockSpec>) {
        for (b in pool.sortedByDescending { it.priority }) {
            val over = total() - avail
            if (over <= 0) return
            val cut = minOf(over, dur.getValue(b.key) - b.minMinutes)
            if (cut > 0) {
                dur[b.key] = dur.getValue(b.key) - cut
                adjustments += Adjustment(AdjustmentKind.SHRUNK, b.key)
            }
        }
    }
    // 1) shrink optional blocks toward their minimum, lowest priority first
    shrink(live.filter { it.droppable && !it.flex })
    // 2) drop optional blocks, lowest priority first
    for (b in live.filter { it.droppable && !it.flex }.sortedByDescending { it.priority }) {
        if (total() - avail <= 0) break
        live.remove(b)
        adjustments += Adjustment(AdjustmentKind.DROPPED, b.key)
    }
    // 3) only then shrink required blocks toward their minimum
    shrink(live.filter { !it.droppable && !it.flex })
    // 4) give the slack to the flex block
    val slack = avail - total()
    if (slack > 0) live.firstOrNull { it.flex }?.let { dur[it.key] = dur.getValue(it.key) + slack }
    if (total() > avail) adjustments += Adjustment(AdjustmentKind.OVERFLOW, null)
    var t = start
    val out = mutableListOf<ResolvedBlock>()
    for (b in live) {
        val d = dur.getValue(b.key)
        if (d <= 0) continue
        out += ResolvedBlock(
            b.key, b.title, b.role, t, t + d, b.isDeepWork, b.isPrayer,
            shrunkFromMinutes = if (!b.flex && d < b.preferredMinutes) b.preferredMinutes else null,
        )
        t += d
    }
    return FittedSegment(start, end, out, adjustments)
}

data class ResolvedDay(
    val date: LocalDate,
    val type: DayType,
    val times: DayMinutes,
    /** Tonight: from today's Isha to tomorrow's Fajr. */
    val plan: NightPlan,
    /** Last night, which decides this morning's qiyam block. */
    val prevPlan: NightPlan,
    val blocks: List<ResolvedBlock>,
    val adjustments: List<Adjustment>,
)

object DayResolver {
    /**
     * [nextFajr] is tomorrow's Fajr in minutes from today's midnight (about 1440 and more);
     * [prevIsha] is yesterday's Isha in yesterday's minutes.
     */
    fun resolve(
        date: LocalDate,
        times: DayMinutes,
        nextFajr: Int,
        prevIsha: Int,
        userBedtime: Int = DEFAULT_BEDTIME,
        qiyamEnabled: Boolean = true,
        type: DayType = if (date.dayOfWeek == DayOfWeek.FRIDAY) DayType.FRIDAY else DayType.WORKDAY,
        /** The user's changes to the template; empty = the reference template exactly. */
        custom: DayCustomization = DayCustomization(),
        /** The day the user built («يومك»); when set it replaces the template and [custom]. */
        myDay: MyDayPlan? = null,
    ): ResolvedDay {
        val plan = nightPlan(times.isha, nextFajr, userBedtime, qiyamEnabled)
        val prevPlan = nightPlan(prevIsha, times.fajr + 1440, userBedtime, qiyamEnabled)
        val anchors = mapOf(
            Anchor.FAJR to times.fajr, Anchor.SUNRISE to times.sunrise, Anchor.DHUHR to times.dhuhr,
            Anchor.ASR to times.asr, Anchor.MAGHRIB to times.maghrib, Anchor.ISHA to times.isha,
            Anchor.BEDTIME to plan.bedtime,
        )
        val blocks = mutableListOf<ResolvedBlock>()
        val adjustments = mutableListOf<Adjustment>()
        if (prevPlan.qiyam) {
            blocks += ResolvedBlock("qiyam", "السَّحَر: ركعتان والوتر", Role.WORSHIP, prevPlan.wake - 1440, times.fajr, false, false)
        }
        val appointments = myDay?.appointmentsOn(date.dayOfWeek) ?: custom.appointmentsOn(date.dayOfWeek)
        val busy = appointments.map { it to (it.start until it.end) }
        val segments = if (myDay != null) myDayTemplate(type, myDay, plan.witrBeforeSleep) else customizedTemplate(type, plan.nap, custom)
        for (seg in segments) {
            val fitted = fitSegmentAround(anchors.getValue(seg.from), anchors.getValue(seg.to), seg.blocks, busy)
            blocks += fitted.blocks
            adjustments += fitted.adjustments
        }
        return ResolvedDay(date, type, times, plan, prevPlan, blocks, adjustments)
    }

    /** Convenience over the engine: yesterday, today and tomorrow in the same city. */
    fun resolve(
        yesterday: DayPrayerTimes,
        today: DayPrayerTimes,
        tomorrow: DayPrayerTimes,
        userBedtime: Int = DEFAULT_BEDTIME,
        qiyamEnabled: Boolean = true,
        custom: DayCustomization = DayCustomization(),
        myDay: MyDayPlan? = null,
    ): ResolvedDay = resolve(
        today.date, DayMinutes.of(today), tomorrow.wallMinutes(Prayer.FAJR) + 1440,
        yesterday.wallMinutes(Prayer.ISHA), userBedtime, qiyamEnabled,
        if (myDay != null) (if (today.date.dayOfWeek == DayOfWeek.FRIDAY) DayType.FRIDAY else DayType.WORKDAY) else custom.dayType(today.date.dayOfWeek),
        custom, myDay,
    )
}

/** A wall-clock minute of [date] (may be 1440 or more) as a local date-time in the city. */
fun toLocalDateTime(date: LocalDate, minutes: Int): LocalDateTime {
    val dayOffset = minutes.floorDiv(1440)
    val m = minutes.mod(1440)
    return LocalDateTime(date.plus(dayOffset, DateTimeUnit.DAY), LocalTime(m / 60, m % 60))
}
