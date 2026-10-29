package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.alarm.AlertPrefs
import com.dcc.yawm.shared.alarm.AlertStyle
import com.dcc.yawm.shared.alarm.ItemAlert
import com.dcc.yawm.shared.alarm.PrayerAlert
import com.dcc.yawm.shared.alarm.UserAlarm
import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.DayOfWeek

/*
 * Notification choices as one setting, same record format as CustomizationCodec. Version 1:
 *   D itemDefault endAlert style morning evening
 *   O key alert                       a per-item reminder
 *   P prayer alert                    a per-prayer choice
 *   M id label minute days on         an alarm of the user's (days as ISO numbers 1..7)
 */

private const val RS = '\u001E'
private const val US = '\u001F'

private inline fun <reified E : Enum<E>> enumOf(s: String): E? = enumValues<E>().firstOrNull { it.name == s }

fun encodeAlerts(a: AlertPrefs): String = buildList {
    add(listOf("D", a.itemDefault.name, a.endAlert.toString(), a.style.name, a.morningSummary.toString(), a.eveningReview.toString()))
    for ((k, v) in a.itemOverrides) add(listOf("O", k, v.name))
    for ((p, v) in a.prayer) add(listOf("P", p.name, v.name))
    for (m in a.alarms) add(listOf("M", m.id, cleanTitle(m.label), m.minute.toString(), m.days.map { it.ordinal + 1 }.sorted().joinToString(","), m.on.toString()))
}.joinToString(RS.toString()) { it.joinToString(US.toString()) }

fun decodeAlerts(s: String?): AlertPrefs {
    if (s.isNullOrEmpty()) return AlertPrefs()
    var a = AlertPrefs()
    val overrides = mutableMapOf<String, ItemAlert>()
    val prayer = mutableMapOf<Prayer, PrayerAlert>()
    val alarms = mutableListOf<UserAlarm>()
    for (rec in s.split(RS)) {
        val f = rec.split(US)
        when (f.firstOrNull()) {
            "D" -> if (f.size == 6) a = a.copy(
                itemDefault = enumOf<ItemAlert>(f[1]) ?: a.itemDefault,
                endAlert = f[2] != "false",
                style = enumOf<AlertStyle>(f[3]) ?: a.style,
                morningSummary = f[4] != "false",
                eveningReview = f[5] != "false",
            )
            "O" -> if (f.size == 3 && f[1].isNotEmpty()) enumOf<ItemAlert>(f[2])?.let { overrides[f[1]] = it }
            "P" -> if (f.size == 3) {
                val p = enumOf<Prayer>(f[1]) ?: continue
                enumOf<PrayerAlert>(f[2])?.let { prayer[p] = it }
            }
            "M" -> if (f.size == 6) {
                val minute = f[3].toIntOrNull()?.takeIf { it in 0 until 1440 } ?: continue
                if (f[1].isEmpty()) continue
                val days = f[4].split(",").mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.map { DayOfWeek.entries[it - 1] }.toSet()
                alarms += UserAlarm(f[1], f[2], minute, days, f[5] != "false")
            }
        }
    }
    return a.copy(itemOverrides = overrides, prayer = prayer, alarms = alarms)
}
