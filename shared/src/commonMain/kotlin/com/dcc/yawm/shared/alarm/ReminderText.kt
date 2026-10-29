package com.dcc.yawm.shared.alarm

import com.dcc.yawm.shared.prayer.Prayer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Notification wording: plain announcements in everyday words, no religious text (CLAUDE.md rule 6).

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
