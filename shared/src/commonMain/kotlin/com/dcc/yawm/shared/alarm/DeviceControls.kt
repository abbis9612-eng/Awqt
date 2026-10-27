package com.dcc.yawm.shared.alarm

import kotlinx.coroutines.flow.StateFlow

/** What the phone allows, read again every time the app comes to the front. */
data class DeviceStatus(
    val notificationsAllowed: Boolean,
    /** Without it alarms are inexact and the adhan may be late (CLAUDE.md notifications). */
    val exactAlarmsAllowed: Boolean,
    val batteryUnrestricted: Boolean,
    val manufacturer: String,
    val scheduledCount: Int,
    /** "العصر 15:12" or null. */
    val next: String?,
)

/** Platform actions the screens need; Android implements it, tests can fake it. */
interface DeviceControls {
    val status: StateFlow<DeviceStatus>
    fun requestNotifications()
    /** Opens "Alarms & reminders" for this app. */
    fun openExactAlarmSettings()
    fun openBatterySettings()
    /** Opens the adhan channel's settings, where the user picks any sound on the phone. */
    fun openAdhanSound()
    fun reschedule()
}

/** Extra steps some makers need beyond Android's own battery setting (from dontkillmyapp.com, 2026-10-27). */
fun batteryAdvice(manufacturer: String): List<String> = when (manufacturer.lowercase()) {
    "xiaomi", "redmi", "poco" -> listOf(
        "الإعدادات ← التطبيقات ← «يوم» ← التشغيل التلقائي: شغّله.",
        "الإعدادات ← التطبيقات ← «يوم» ← توفير البطارية: بلا قيود.",
        "بقائمة التطبيقات الأخيرة اسحب «يوم» لتحت أو اضغط القفل حتى ما ينسد.",
    )
    "samsung" -> listOf(
        "الإعدادات ← التطبيقات ← «يوم» ← البطارية: غير مقيّد.",
        "العناية بالجهاز ← البطارية ← حدود استخدام الخلفية: شيل «يوم» من التطبيقات النايمة، وضيفه للتطبيقات اللي ما تنام أبداً.",
    )
    "huawei", "honor" -> listOf(
        "الإعدادات ← البطارية ← تشغيل التطبيقات ← «يوم»: إدارة يدوية، وشغّل الخيارات الثلاثة.",
    )
    "oppo", "realme", "oneplus", "vivo", "iqoo" -> listOf(
        "الإعدادات ← التطبيقات ← «يوم» ← استخدام البطارية: اسمح بالنشاط بالخلفية والتشغيل التلقائي.",
        "بقائمة التطبيقات الأخيرة اقفل «يوم» حتى ما ينسد.",
    )
    else -> listOf("الإعدادات ← التطبيقات ← «يوم» ← البطارية: بلا قيود.")
}
