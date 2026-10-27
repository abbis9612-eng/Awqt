package com.dcc.yawm.shared.day

import kotlinx.datetime.DayOfWeek

/*
 * The user's own day, laid over the reference template (owner's decision, 2026-10-27): rename,
 * resize, hide and reorder blocks, add their own blocks, pick working days, and pin fixed
 * appointments. Prayers never move and never disappear. With no customization the template is
 * exactly the reference's (CLAUDE.md rule 7); tests check both.
 */

/** How much a block of the user's own matters; decides how it shrinks and drops. */
enum class Importance(val priority: Int, val droppable: Boolean) {
    MUST(2, false), NORMAL(4, true), IF_TIME(6, true),
}

/** A change to one template block. Null keeps the template's value. */
data class BlockChange(val title: String? = null, val minutes: Int? = null, val hidden: Boolean = false)

/** A block the user added inside the segment that starts at [segment]. */
data class CustomBlock(
    val key: String,
    val segment: Anchor,
    val title: String,
    val minutes: Int,
    val importance: Importance,
    val role: Role = Role.SELF,
)

/** Something at a fixed clock time on chosen weekdays, e.g. work from 08:00 to 14:00. */
data class FixedAppointment(
    val id: String,
    val title: String,
    /** Minutes from midnight. */
    val start: Int,
    val end: Int,
    val days: Set<DayOfWeek>,
    val role: Role = Role.BUSINESS,
)

/** Every weekday except Friday works by default, as the reference does (Saturday included). */
val DefaultWorkdays: Set<DayOfWeek> = DayOfWeek.entries.toSet() - DayOfWeek.FRIDAY

data class DayCustomization(
    val changes: Map<DayType, Map<String, BlockChange>> = emptyMap(),
    /** Block keys in the user's order, per day type and segment; prayers always stay first. */
    val order: Map<DayType, Map<Anchor, List<String>>> = emptyMap(),
    val custom: Map<DayType, List<CustomBlock>> = emptyMap(),
    val appointments: List<FixedAppointment> = emptyList(),
    val workdays: Set<DayOfWeek> = DefaultWorkdays,
) {
    fun dayType(day: DayOfWeek): DayType = when {
        day == DayOfWeek.FRIDAY -> DayType.FRIDAY
        day in workdays -> DayType.WORKDAY
        else -> DayType.OFFDAY
    }

    fun appointmentsOn(day: DayOfWeek): List<FixedAppointment> =
        appointments.filter { day in it.days && it.end > it.start }.sortedBy { it.start }

    /** The next free key for a block of the user's own ("u1", "u2", ...). */
    fun nextCustomKey(): String {
        val used = custom.values.flatten().mapNotNull { it.key.removePrefix("u").toIntOrNull() }
        return "u${(used.maxOrNull() ?: 0) + 1}"
    }
}

/** Minutes a user may give a block. */
val BlockMinutesRange = 5..360

/** The template for [type] with the user's changes applied. */
fun customizedTemplate(type: DayType, napMinutes: Int, c: DayCustomization): List<SegmentSpec> {
    val base = Templates.forDay(type, napMinutes)
    val changes = c.changes[type].orEmpty()
    val order = c.order[type].orEmpty()
    val custom = c.custom[type].orEmpty()
    if (changes.isEmpty() && order.isEmpty() && custom.isEmpty()) return base
    return base.map { seg ->
        val own = custom.filter { it.segment == seg.from }.map { b ->
            val m = b.minutes.coerceIn(BlockMinutesRange)
            BlockSpec(b.key, b.title, b.role, m, minOf(m, maxOf(5, m / 2)), b.importance.priority, droppable = b.importance.droppable)
        }
        // The user's blocks go before the flex block, which keeps taking what is left.
        val flexAt = seg.blocks.indexOfFirst { it.flex }.let { if (it < 0) seg.blocks.size else it }
        val all = seg.blocks.take(flexAt) + own + seg.blocks.drop(flexAt)
        val shown = all.mapNotNull { b ->
            val ch = changes[b.key] ?: return@mapNotNull b
            if (ch.hidden && !b.isPrayer) return@mapNotNull null
            val title = ch.title?.trim()?.takeIf { it.isNotEmpty() } ?: b.title
            // A prayer is never shorter than 10 minutes (CLAUDE.md invariant 2).
            val minutes = ch.minutes?.coerceIn(BlockMinutesRange)?.let { if (b.isPrayer) maxOf(it, 10) else it }
            if (minutes == null || b.flex || b.key == "nap") b.copy(title = title)
            else b.copy(title = title, preferredMinutes = minutes, minMinutes = minOf(b.minMinutes, minutes))
        }
        val wanted = order[seg.from]
        val sorted = if (wanted == null) shown else {
            val prayer = shown.filter { it.isPrayer }
            val rest = shown.filter { !it.isPrayer }
            prayer + rest.sortedBy { b -> wanted.indexOf(b.key).let { if (it < 0) Int.MAX_VALUE else it } }
        }
        seg.copy(blocks = sorted)
    }
}

/**
 * Fits a segment around fixed appointments. The prayer block keeps its exact start (shortened to
 * its minimum if an appointment begins right after it); appointment time is never given to other
 * blocks; the remaining blocks fill the free pieces in order. Without an overlapping appointment
 * this is exactly [fitSegment].
 */
fun fitSegmentAround(start: Int, end: Int, specs: List<BlockSpec>, busy: List<Pair<FixedAppointment, IntRange>>): FittedSegment {
    val inside = busy.mapNotNull { (a, r) ->
        val s = maxOf(r.first, start)
        val e = minOf(r.last + 1, end)
        if (e > s) Triple(a, s, e) else null
    }
    if (inside.isEmpty()) return fitOrDrop(start, end, specs)

    val out = mutableListOf<ResolvedBlock>()
    val adjustments = mutableListOf<Adjustment>()
    var rest = specs
    var from = start
    val prayer = specs.firstOrNull()?.takeIf { it.isPrayer }
    if (prayer != null) {
        val firstBusy = inside.minOf { it.second }
        val room = (firstBusy - start).coerceAtLeast(0)
        val len = when {
            room >= prayer.preferredMinutes -> prayer.preferredMinutes
            else -> prayer.minMinutes.coerceAtMost(end - start)
        }
        if (len < prayer.preferredMinutes) adjustments += Adjustment(AdjustmentKind.SHRUNK, prayer.key)
        out += ResolvedBlock(
            prayer.key, prayer.title, prayer.role, start, start + len, prayer.isDeepWork, true,
            shrunkFromMinutes = if (len < prayer.preferredMinutes) prayer.preferredMinutes else null,
        )
        from = start + len
        rest = specs.drop(1)
    }

    // Appointment blocks, cut by the prayer if they overlap it; when two overlap, the later one
    // starts where the earlier one ends.
    val appts = mutableListOf<Triple<FixedAppointment, Int, Int>>()
    var taken = from
    for ((a, s, e) in inside.sortedBy { it.second }) {
        val s2 = maxOf(s, taken)
        if (e > s2) {
            appts += Triple(a, s2, e)
            taken = e
        }
    }
    for ((a, s, e) in appts) out += ResolvedBlock("fixed:${a.id}", a.title, a.role, s, e, false, false)

    // Free pieces between appointments.
    val pieces = mutableListOf<IntRange>()
    var t = from
    for ((_, s, e) in appts) {
        if (s > t) pieces += t until s
        t = maxOf(t, e)
    }
    if (end > t) pieces += t until end

    // Hand the blocks out to the pieces in order, as many as each can hold at their usual length.
    val assigned = pieces.map { mutableListOf<BlockSpec>() }
    var p = 0
    var used = 0
    for (b in rest) {
        val need = if (b.flex) b.minMinutes else b.preferredMinutes
        while (p < pieces.lastIndex && used > 0 && used + need > pieces[p].count()) { p++; used = 0 }
        if (pieces.isEmpty()) {
            adjustments += Adjustment(AdjustmentKind.DROPPED, b.key)
            continue
        }
        assigned[p] += b
        used += need
    }
    pieces.forEachIndexed { i, piece ->
        if (assigned[i].isEmpty()) return@forEachIndexed
        val f = fitOrDrop(piece.first, piece.last + 1, assigned[i])
        adjustments += f.adjustments
        for (b in f.blocks) {
            // Never run into the next appointment: cut, or drop what no longer fits.
            val e = minOf(b.end, piece.last + 1)
            if (e > b.start) out += if (e < b.end) b.copy(end = e, shrunkFromMinutes = b.shrunkFromMinutes ?: (b.end - b.start)) else b
            else adjustments += Adjustment(AdjustmentKind.DROPPED, b.key)
        }
    }
    return FittedSegment(start, end, out.sortedBy { it.start }, adjustments)
}

/**
 * [fitSegment], and if the blocks still overflow after every shrink (only possible with the
 * user's own blocks or lengths; never with the reference template), drop the least important
 * non-prayer block and fit again, so nothing runs into the next prayer. Without overflow this
 * returns exactly what [fitSegment] returns.
 */
fun fitOrDrop(start: Int, end: Int, specs: List<BlockSpec>): FittedSegment {
    var live = specs
    val dropped = mutableListOf<Adjustment>()
    while (true) {
        val f = fitSegment(start, end, live)
        if (f.adjustments.none { it.kind == AdjustmentKind.OVERFLOW }) {
            return if (dropped.isEmpty()) f else f.copy(adjustments = dropped + f.adjustments)
        }
        val victim = live.withIndex().filter { !it.value.isPrayer }
            .maxWithOrNull(compareBy<IndexedValue<BlockSpec>> { it.value.priority }.thenBy { it.index })?.value
        if (victim == null) {
            // Only the prayer is left and it is longer than the segment: keep its start, cut its end.
            val cut = f.blocks.mapNotNull { b -> if (b.start >= end) null else b.copy(end = minOf(b.end, end)) }
            return f.copy(blocks = cut, adjustments = dropped + f.adjustments.filter { it.kind != AdjustmentKind.OVERFLOW })
        }
        live = live - victim
        dropped += Adjustment(AdjustmentKind.DROPPED, victim.key)
    }
}
