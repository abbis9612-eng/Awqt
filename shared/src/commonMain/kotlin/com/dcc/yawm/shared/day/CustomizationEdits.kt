package com.dcc.yawm.shared.day

import kotlinx.datetime.DayOfWeek

// The edits the «رتّب يومك» screen makes. Each returns a new customization.

/** A block as the editor lists it: hidden ones stay listed so they can be shown again. */
data class EditorBlock(val spec: BlockSpec, val hidden: Boolean, val isOwn: Boolean)

/** The template of [type] with the user's changes, hidden blocks included and marked. */
fun DayCustomization.editorView(type: DayType): List<Pair<SegmentSpec, List<EditorBlock>>> {
    val unhidden = copy(changes = changes.mapValues { (_, m) -> m.mapValues { (_, c) -> c.copy(hidden = false) } })
    val own = custom[type].orEmpty().map { it.key }.toSet()
    val hiddenKeys = changes[type].orEmpty().filterValues { it.hidden }.keys
    return customizedTemplate(type, SHORT_NAP, unhidden).map { seg ->
        seg to seg.blocks.map { EditorBlock(it, !it.isPrayer && it.key in hiddenKeys, it.key in own) }
    }
}

fun DayCustomization.withChange(type: DayType, key: String, change: (BlockChange) -> BlockChange): DayCustomization {
    val m = changes[type].orEmpty()
    return copy(changes = changes + (type to (m + (key to change(m[key] ?: BlockChange())))))
}

/** Moves a non-prayer block one place earlier ([delta] = -1) or later (+1) inside its segment. */
fun DayCustomization.moved(type: DayType, segment: Anchor, key: String, delta: Int): DayCustomization {
    val seg = editorView(type).firstOrNull { it.first.from == segment } ?: return this
    val keys = seg.second.filter { !it.spec.isPrayer }.map { it.spec.key }.toMutableList()
    val i = keys.indexOf(key)
    val j = i + delta
    if (i < 0 || j !in keys.indices) return this
    keys[i] = keys[j].also { keys[j] = keys[i] }
    val m = order[type].orEmpty()
    return copy(order = order + (type to (m + (segment to keys))))
}

fun DayCustomization.withOwnBlock(type: DayType, block: CustomBlock): DayCustomization =
    copy(custom = custom + (type to (custom[type].orEmpty().filter { it.key != block.key } + block)))

fun DayCustomization.withoutOwnBlock(type: DayType, key: String): DayCustomization =
    copy(
        custom = custom + (type to custom[type].orEmpty().filter { it.key != key }),
        changes = changes + (type to changes[type].orEmpty() - key),
    )

/** Back to the template for this kind of day. */
fun DayCustomization.reset(type: DayType): DayCustomization =
    copy(changes = changes - type, order = order - type, custom = custom - type)

fun DayCustomization.toggledWorkday(day: DayOfWeek): DayCustomization =
    if (day == DayOfWeek.FRIDAY) this
    else copy(workdays = if (day in workdays) workdays - day else workdays + day)

fun DayCustomization.withAppointment(a: FixedAppointment): DayCustomization =
    copy(appointments = appointments.filter { it.id != a.id } + a)

fun DayCustomization.withoutAppointment(id: String): DayCustomization =
    copy(appointments = appointments.filter { it.id != id })

fun DayCustomization.nextAppointmentId(): String =
    "a${(appointments.mapNotNull { it.id.removePrefix("a").toIntOrNull() }.maxOrNull() ?: 0) + 1}"
