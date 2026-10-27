package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.day.Anchor
import com.dcc.yawm.shared.day.BlockChange
import com.dcc.yawm.shared.day.CustomBlock
import com.dcc.yawm.shared.day.DayCustomization
import com.dcc.yawm.shared.day.DayType
import com.dcc.yawm.shared.day.DefaultWorkdays
import com.dcc.yawm.shared.day.FixedAppointment
import com.dcc.yawm.shared.day.Importance
import com.dcc.yawm.shared.day.Role
import kotlinx.datetime.DayOfWeek

/*
 * The user's day, stored as one setting. Records are split by RS (U+001E) and fields by US
 * (U+001F), which no title can contain (they are removed when typed). Unreadable records are
 * skipped one by one, never crash. Format version 1:
 *   C type key title minutes hidden      a change to a template block
 *   O type anchor key,key,...             block order in a segment
 *   B type key anchor title minutes importance role
 *   A id title start end days role        a fixed appointment (days as ISO numbers 1..7)
 *   W days                                working days
 */

private const val RS = '\u001E'
private const val US = '\u001F'

/** Removes the separators (and trims) so a typed title can always be stored. */
fun cleanTitle(s: String): String = s.filter { it != RS && it != US && it != '\n' }.trim().take(60)

private fun days(s: String): Set<DayOfWeek> =
    s.split(",").mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.map { DayOfWeek.entries[it - 1] }.toSet()

private fun daysText(d: Set<DayOfWeek>) = d.map { it.ordinal + 1 }.sorted().joinToString(",")

private inline fun <reified E : Enum<E>> enumOf(s: String): E? = enumValues<E>().firstOrNull { it.name == s }

fun encodeCustomization(c: DayCustomization): String = buildList {
    for ((type, m) in c.changes) for ((key, ch) in m) {
        add(listOf("C", type.name, key, ch.title.orEmpty(), ch.minutes?.toString().orEmpty(), ch.hidden.toString()))
    }
    for ((type, m) in c.order) for ((anchor, keys) in m) add(listOf("O", type.name, anchor.name, keys.joinToString(",")))
    for ((type, list) in c.custom) for (b in list) {
        add(listOf("B", type.name, b.key, b.segment.name, b.title, b.minutes.toString(), b.importance.name, b.role.name))
    }
    for (a in c.appointments) {
        add(listOf("A", a.id, a.title, a.start.toString(), a.end.toString(), daysText(a.days), a.role.name))
    }
    add(listOf("W", daysText(c.workdays)))
}.joinToString(RS.toString()) { it.joinToString(US.toString()) }

fun decodeCustomization(s: String?): DayCustomization {
    if (s.isNullOrEmpty()) return DayCustomization()
    val changes = mutableMapOf<DayType, MutableMap<String, BlockChange>>()
    val order = mutableMapOf<DayType, MutableMap<Anchor, List<String>>>()
    val custom = mutableMapOf<DayType, MutableList<CustomBlock>>()
    val appointments = mutableListOf<FixedAppointment>()
    var workdays = DefaultWorkdays
    for (rec in s.split(RS)) {
        val f = rec.split(US)
        when (f.firstOrNull()) {
            "C" -> if (f.size == 6) {
                val type = enumOf<DayType>(f[1]) ?: continue
                changes.getOrPut(type) { mutableMapOf() }[f[2]] =
                    BlockChange(f[3].ifEmpty { null }, f[4].toIntOrNull(), f[5] == "true")
            }
            "O" -> if (f.size == 4) {
                val type = enumOf<DayType>(f[1]) ?: continue
                val anchor = enumOf<Anchor>(f[2]) ?: continue
                order.getOrPut(type) { mutableMapOf() }[anchor] = f[3].split(",").filter { it.isNotEmpty() }
            }
            "B" -> if (f.size == 8) {
                val type = enumOf<DayType>(f[1]) ?: continue
                val anchor = enumOf<Anchor>(f[3]) ?: continue
                val minutes = f[5].toIntOrNull() ?: continue
                if (f[4].isBlank()) continue
                custom.getOrPut(type) { mutableListOf() } +=
                    CustomBlock(f[2], anchor, f[4], minutes, enumOf<Importance>(f[6]) ?: Importance.NORMAL, enumOf<Role>(f[7]) ?: Role.SELF)
            }
            "A" -> if (f.size == 7) {
                val start = f[3].toIntOrNull() ?: continue
                val end = f[4].toIntOrNull() ?: continue
                if (start !in 0 until 1440 || end !in 1..1440 || end <= start || f[2].isBlank()) continue
                appointments += FixedAppointment(f[1], f[2], start, end, days(f[5]), enumOf<Role>(f[6]) ?: Role.BUSINESS)
            }
            "W" -> if (f.size == 2) workdays = days(f[1])
        }
    }
    return DayCustomization(changes, order, custom, appointments, workdays)
}
