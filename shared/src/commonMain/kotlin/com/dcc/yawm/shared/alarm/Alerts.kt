package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.day.Repeat
import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/*
 * What the user chose for notifications (owner, 2026-10-28 and 2026-10-29): a reminder before
 * each item (at its start, 5 or 10 minutes before, or none, changeable per item), an alert when
 * an item ends, a choice per prayer (adhan, a plain alert, or nothing; plain by default), their
 * own alarms, a morning summary and an evening review.
 */

enum class ItemAlert(val label: String) { START("بوقتها"), BEFORE5("قبل 5"), BEFORE10("قبل 10"), OFF("بدون") }

enum class PrayerAlert(val label: String) { ADHAN("أذان"), NOTIFY("تنبيه"), NONE("بدون") }

/** How non-alarm notifications reach the user. */
enum class AlertStyle(val label: String) { SOUND("صوت"), VIBRATE("هزّة"), SILENT("إشعار بس") }

data class UserAlarm(
    val id: String,
    val label: String,
    /** Minutes from midnight. */
    val minute: Int,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val on: Boolean = true,
    /** A one-time alarm rings only on this day; null = it repeats on [days]. */
    val onceOn: LocalDate? = null,
) {
    fun ringsOn(d: LocalDate): Boolean = on && if (onceOn != null) d == onceOn else d.dayOfWeek in days
}

/**
 * An alarm from the add sheet: daily, on working days, or once. A one-time alarm whose time has
 * already passed today rings tomorrow (the sheet says so before it is saved).
 */
fun newAlarm(id: String, label: String, minute: Int, repeat: Repeat, workDays: Set<DayOfWeek>, today: LocalDate, nowMinute: Int): UserAlarm = when (repeat) {
    Repeat.DAILY -> UserAlarm(id, label, minute)
    Repeat.WORKDAYS -> UserAlarm(id, label, minute, workDays)
    Repeat.ONCE -> UserAlarm(id, label, minute, onceOn = if (minute > nowMinute) today else today.plus(1, DateTimeUnit.DAY))
}

data class AlertPrefs(
    val itemDefault: ItemAlert = ItemAlert.START,
    /** Per item key (a template key or "my:<id>"). */
    val itemOverrides: Map<String, ItemAlert> = emptyMap(),
    val endAlert: Boolean = true,
    val style: AlertStyle = AlertStyle.SOUND,
    val prayer: Map<Prayer, PrayerAlert> = emptyMap(),
    val alarms: List<UserAlarm> = emptyList(),
    val morningSummary: Boolean = true,
    val eveningReview: Boolean = true,
) {
    fun itemAlert(key: String): ItemAlert = itemOverrides[key] ?: itemDefault
    fun prayerAlert(p: Prayer): PrayerAlert = prayer[p] ?: PrayerAlert.NOTIFY

    fun nextAlarmId(): String = "a${(alarms.mapNotNull { it.id.removePrefix("a").toIntOrNull() }.maxOrNull() ?: 0) + 1}"
}
