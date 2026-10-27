package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.day.WAKE_BEFORE_FAJR_PLAIN
import com.dcc.yawm.shared.day.WAKE_BEFORE_FAJR_QIYAM
import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Notification wording. Plain announcements, no religious text (CLAUDE.md rule 6).
// Provisional until the owner approves it (docs/DECISIONS.md).

fun prayerName(p: Prayer): String = when (p) {
    Prayer.FAJR -> "الفجر"
    Prayer.SUNRISE -> "الشروق"
    Prayer.DHUHR -> "الظهر"
    Prayer.ASR -> "العصر"
    Prayer.MAGHRIB -> "المغرب"
    Prayer.ISHA -> "العشاء"
}

fun clockText(at: Instant, zone: TimeZone): String {
    val t = at.toLocalDateTime(zone)
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

data class ReminderText(val title: String, val body: String)

fun reminderText(r: Reminder, zone: TimeZone): ReminderText = when (r.kind) {
    ReminderKind.ADHAN -> ReminderText("حان وقت صلاة ${prayerName(r.prayer ?: Prayer.FAJR)}", "الساعة ${clockText(r.at, zone)}")
    ReminderKind.WAKE ->
        if (r.title == "qiyam") ReminderText("وقت الكعدة للسَّحَر", "الفجر بعد $WAKE_BEFORE_FAJR_QIYAM دقيقة")
        else ReminderText("وقت الكعدة قبل الفجر", "الفجر بعد $WAKE_BEFORE_FAJR_PLAIN دقائق")
    ReminderKind.BLOCK -> ReminderText(
        r.title,
        "من ${clockText(r.at, zone)}" + (r.until?.let { " إلى ${clockText(it, zone)}" } ?: ""),
    )
}
