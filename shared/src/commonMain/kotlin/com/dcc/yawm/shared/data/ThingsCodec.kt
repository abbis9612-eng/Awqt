package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.day.DayPart
import com.dcc.yawm.shared.day.Remind
import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.day.Thing
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/*
 * Things («يومك بين الصلوات») as one setting, same record format as CustomizationCodec.
 * Version 1, one record per thing:
 *   H id title part date at end repeat remind done   (empty field = none; done as comma ISO dates)
 * Working days, as ISO numbers 1..7, are their own setting.
 */

private const val RS = '\u001E'
private const val US = '\u001F'

private inline fun <reified E : Enum<E>> enumOf(s: String): E? = enumValues<E>().firstOrNull { it.name == s }
private fun date(s: String): LocalDate? = if (s.isEmpty()) null else runCatching { LocalDate.parse(s) }.getOrNull()
private fun minute(s: String, max: Int): Int? = s.toIntOrNull()?.takeIf { it in 0..max }

fun encodeThings(things: List<Thing>): String = things.joinToString(RS.toString()) { t ->
    listOf(
        "H", t.id, cleanTitle(t.title), t.part?.name.orEmpty(), t.date?.toString().orEmpty(), t.at?.toString().orEmpty(), t.end?.toString().orEmpty(),
        t.repeat.name, t.remind.name, t.done.sorted().joinToString(","),
    ).joinToString(US.toString())
}

fun decodeThings(s: String?): List<Thing> {
    if (s.isNullOrEmpty()) return emptyList()
    return s.split(RS).mapNotNull { rec ->
        val f = rec.split(US)
        if (f.size != 10 || f[0] != "H" || f[1].isEmpty() || f[2].isBlank()) return@mapNotNull null
        Thing(
            id = f[1], title = f[2], part = enumOf<DayPart>(f[3]), date = date(f[4]),
            at = minute(f[5], 1439), end = minute(f[6], 1440),
            repeat = enumOf<Repeat>(f[7]) ?: Repeat.ONCE, remind = enumOf<Remind>(f[8]) ?: Remind.START,
            done = f[9].split(",").mapNotNull { date(it) }.toSet(),
        )
    }
}

fun encodeDays(d: Set<DayOfWeek>): String = d.map { it.ordinal + 1 }.sorted().joinToString(",")

fun decodeDays(s: String?): Set<DayOfWeek>? =
    s?.split(",")?.mapNotNull { it.toIntOrNull() }?.filter { it in 1..7 }?.map { DayOfWeek.entries[it - 1] }?.toSet()
