package com.dcc.yawm.shared.screen

import com.dcc.yawm.shared.day.DayPart

/*
 * «الألوان» (approved 2026-11-05, docs/design/yawm-colors.html): the screen takes the fixed colour of the
 * part you are in, and the bottom bar is the whole day as one spectrum, each part at its real length
 * and the night after it, from this day's Fajr to the next. Computed here from the same parts and night
 * plan as before; the colours themselves live in ui/DesignTokens.kt.
 */

/** The six parts of the day and the night; each has one fixed colour. */
enum class Field(val short: String) {
    FAJR("فجر"), MORNING("صبح"), DHUHR("ظهر"), ASR("عصر"), MAGHRIB("مغرب"), ISHA("عشاء"), NIGHT("ليل");

    companion object {
        fun of(part: DayPart): Field = entries[part.ordinal]
    }
}

data class Segment(val field: Field, val start: Int, val end: Int) {
    val length: Int get() = end - start
}

class Spectrum(val segments: List<Segment>) {
    val start: Int get() = segments.first().start
    val end: Int get() = segments.last().end
    val total: Int get() = end - start

    fun fraction(s: Segment): Float = s.length.toFloat() / total

    /** The field of app minute [t] (after bedtime and until the next Fajr, the night). */
    fun fieldAt(t: Int): Field = segments.firstOrNull { t >= it.start && t < it.end }?.field ?: Field.NIGHT

    /** Where [t] sits on the bar, 0 at its start edge (Fajr) and 1 at its end (the next Fajr). */
    fun needle(t: Int): Float = ((t - start).toFloat() / total).coerceIn(0f, 1f)

    /** The field under a point [f] of the bar's length from its start edge. */
    fun fieldAtFraction(f: Float): Field {
        var acc = 0f
        for (s in segments) {
            acc += fraction(s)
            if (f < acc) return s.field
        }
        return segments.last().field
    }

    fun segment(f: Field): Segment = segments.first { it.field == f }
}

/** The day of [view] as a spectrum: its six parts, then the night until the next Fajr. */
fun spectrum(view: PartsView): Spectrum {
    val parts = view.parts.map { Segment(Field.of(it.part), it.span.start, it.span.end) }
    val nightStart = parts.last().end
    return Spectrum(parts + Segment(Field.NIGHT, nightStart, maxOf(view.day.nextFajr, nightStart + 1)))
}

/** The part of a field, null for the night. */
fun PartsView.partOf(f: Field): PartOfDay? = if (f == Field.NIGHT) null else parts.first { Field.of(it.part) == f }

/** The big name of a field, one word per line. */
fun fieldName(f: Field): List<String> = if (f == Field.NIGHT) listOf("الليل") else DayPart.entries[f.ordinal].label.split(' ')

/**
 * The one sentence under the name: what is left of the part you are in, how a past part went,
 * when a later part begins, or the night's times.
 */
fun fieldLine(view: PartsView, spec: Spectrum, f: Field, t: Int): String {
    if (f == Field.NIGHT) {
        return "تنام ${clock(view.bedtime)}، وتصحى ${clock(view.wake)}" + if (view.day.plan.qiyam) " لصلاة الليل." else "."
    }
    val p = view.partOf(f)!!
    val s = p.span
    val n = p.things.size
    val d = p.things.count { it.doneOn(view.date) }
    return when {
        t >= s.start && t < s.end -> "باقي ${durText(s.end - t)} على ${p.part.endLabel}، ${clock(s.end)}."
        s.end <= t -> if (n == 0) "خلص." else "خلص. خلّصت $d من $n."
        else -> "يبدي ${clock(s.start)}."
    }
}
