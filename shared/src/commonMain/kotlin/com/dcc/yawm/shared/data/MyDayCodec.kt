package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.day.ItemAnchor
import com.dcc.yawm.shared.day.LifeKind
import com.dcc.yawm.shared.day.MyDayPlan
import com.dcc.yawm.shared.day.MyItem
import com.dcc.yawm.shared.day.TaskItem
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/*
 * The user's own day and tasks, each stored as one setting in the same record format as
 * CustomizationCodec (RS between records, US between fields; unreadable records are skipped).
 * Day, format version 1:
 *   L life workStart workEnd days         life kind and work hours (days as ISO numbers 1..7)
 *   I id kind title icon anchor at minutes important
 * Tasks, format version 1:
 *   T id title minutes day pinAt doneOn   (day and doneOn as ISO dates or empty)
 */

private const val RS = '\u001E'
private const val US = '\u001F'

private inline fun <reified E : Enum<E>> enumOf(s: String): E? = enumValues<E>().firstOrNull { it.name == s }

private fun days(s: String): Set<DayOfWeek> =
    s.split(",").mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.map { DayOfWeek.entries[it - 1] }.toSet()

private fun daysText(d: Set<DayOfWeek>) = d.map { it.ordinal + 1 }.sorted().joinToString(",")

private fun date(s: String): LocalDate? = if (s.isEmpty()) null else runCatching { LocalDate.parse(s) }.getOrNull()

fun encodeMyDay(p: MyDayPlan?): String = if (p == null) "" else buildList {
    add(listOf("L", p.life.name, p.workStart.toString(), p.workEnd.toString(), daysText(p.workDays)))
    for (i in p.items) add(listOf("I", i.id, i.kind, cleanTitle(i.title), i.icon, i.anchor.name, i.at.toString(), i.minutes.toString(), i.important.toString()))
}.joinToString(RS.toString()) { it.joinToString(US.toString()) }

/** Empty means the user has not built a day: the ready template is used. */
fun decodeMyDay(s: String?): MyDayPlan? {
    if (s.isNullOrEmpty()) return null
    var plan = MyDayPlan()
    val items = mutableListOf<MyItem>()
    for (rec in s.split(RS)) {
        val f = rec.split(US)
        when (f.firstOrNull()) {
            "L" -> if (f.size == 5) {
                val start = f[2].toIntOrNull()?.takeIf { it in 0 until 1440 } ?: continue
                val end = f[3].toIntOrNull()?.takeIf { it in 1..1440 } ?: continue
                plan = plan.copy(life = enumOf<LifeKind>(f[1]) ?: LifeKind.FREE, workStart = start, workEnd = end, workDays = days(f[4]))
            }
            "I" -> if (f.size == 9) {
                val anchor = enumOf<ItemAnchor>(f[5]) ?: continue
                val at = f[6].toIntOrNull()?.takeIf { it in 0 until 1440 } ?: continue
                val minutes = f[7].toIntOrNull()?.takeIf { it in 5..360 } ?: continue
                if (f[1].isEmpty() || f[3].isBlank()) continue
                items += MyItem(f[1], f[2], f[3], f[4], anchor, at, minutes, f[8] == "true")
            }
        }
    }
    return plan.copy(items = items)
}

fun encodeTasks(tasks: List<TaskItem>): String = tasks.joinToString(RS.toString()) { t ->
    listOf("T", t.id, cleanTitle(t.title), t.minutes.toString(), t.day?.toString().orEmpty(), t.pinAt?.toString().orEmpty(), t.doneOn?.toString().orEmpty())
        .joinToString(US.toString())
}

fun decodeTasks(s: String?): List<TaskItem> {
    if (s.isNullOrEmpty()) return emptyList()
    return s.split(RS).mapNotNull { rec ->
        val f = rec.split(US)
        if (f.size != 7 || f[0] != "T" || f[1].isEmpty() || f[2].isBlank()) return@mapNotNull null
        val minutes = f[3].toIntOrNull()?.takeIf { it in 5..600 } ?: return@mapNotNull null
        TaskItem(f[1], f[2], minutes, date(f[4]), f[5].toIntOrNull()?.takeIf { it in 0 until 1440 }, date(f[6]))
    }
}
