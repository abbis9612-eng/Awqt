package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.day.DEFAULT_BEDTIME
import com.dcc.yawm.shared.day.Role
import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.MosqueAdjustments
import com.dcc.yawm.shared.prayer.PrayerSettings

/**
 * Everything the user chooses. The defaults are the owner's city until the first-run screen
 * exists (step 7). Only city coordinates are stored (CLAUDE.md rule 5).
 */
data class UserSettings(
    val cityName: String = "السليمانية",
    val place: GeoPoint = GeoPoint(35.5613, 45.4375),
    val zoneId: String = "Asia/Baghdad",
    val prayer: PrayerSettings = PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI),
    /** Minutes from midnight. */
    val bedtime: Int = DEFAULT_BEDTIME,
    val qiyamEnabled: Boolean = true,
    val roles: Set<Role> = Role.entries.toSet(),
    /** A notification when each non-prayer block starts. */
    val blockReminders: Boolean = true,
    /** The wake alarm before Fajr (for qiyam or plain waking), shown as the system's next alarm. */
    val wakeAlarm: Boolean = true,
    /** Light or dark; AUTO is dark from Isha to Fajr (docs/design/IDENTITY.md). */
    val theme: Theme = Theme.AUTO,
    /** False until the first-run screen is finished. */
    val setupDone: Boolean = false,
)

enum class Theme { AUTO, LIGHT, DARK }

// Keys never change once shipped: renaming one would silently reset the user's choice.
private const val CITY = "city.name"
private const val LAT = "city.latitude"
private const val LON = "city.longitude"
private const val ZONE = "city.zone"
private const val METHOD = "prayer.method"
private const val MADHAB = "prayer.madhab"
private const val HIGH_LAT = "prayer.highLatitudeRule"
private const val ADJ = "prayer.adjust."
private const val BEDTIME = "night.bedtime"
private const val QIYAM = "night.qiyam"
private const val ROLES = "roles"
private const val BLOCK_REMINDERS = "alerts.blocks"
private const val WAKE_ALARM = "alerts.wake"
private const val THEME = "look.theme"
private const val SETUP_DONE = "setup.done"
private const val AUTO = "auto"

internal fun encodeSettings(s: UserSettings): Map<String, String> {
    val a = s.prayer.mosqueAdjustments
    return mapOf(
        CITY to s.cityName,
        LAT to s.place.latitude.toString(),
        LON to s.place.longitude.toString(),
        ZONE to s.zoneId,
        METHOD to s.prayer.method.name,
        MADHAB to s.prayer.madhab.name,
        HIGH_LAT to (s.prayer.highLatitudeRule?.name ?: AUTO),
        ADJ + "fajr" to a.fajr.toString(),
        ADJ + "sunrise" to a.sunrise.toString(),
        ADJ + "dhuhr" to a.dhuhr.toString(),
        ADJ + "asr" to a.asr.toString(),
        ADJ + "maghrib" to a.maghrib.toString(),
        ADJ + "isha" to a.isha.toString(),
        BEDTIME to s.bedtime.toString(),
        QIYAM to s.qiyamEnabled.toString(),
        ROLES to s.roles.sorted().joinToString(",") { it.name },
        BLOCK_REMINDERS to s.blockReminders.toString(),
        WAKE_ALARM to s.wakeAlarm.toString(),
        THEME to s.theme.name,
        SETUP_DONE to s.setupDone.toString(),
    )
}

/** A missing or unreadable value falls back to its default, never crashes the app. */
internal fun decodeSettings(m: Map<String, String>): UserSettings {
    val d = UserSettings()
    val lat = m[LAT]?.toDoubleOrNull()
    val lon = m[LON]?.toDoubleOrNull()
    val place = if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) GeoPoint(lat, lon) else d.place
    fun adj(name: String, default: Int) = m[ADJ + name]?.toIntOrNull()?.takeIf { it in -60..60 } ?: default
    val da = d.prayer.mosqueAdjustments
    return UserSettings(
        cityName = m[CITY]?.takeIf { it.isNotBlank() } ?: d.cityName,
        place = place,
        zoneId = m[ZONE]?.takeIf { it.isNotBlank() } ?: d.zoneId,
        prayer = PrayerSettings(
            method = m[METHOD]?.let { v -> CalcMethod.entries.firstOrNull { it.name == v } } ?: d.prayer.method,
            madhab = m[MADHAB]?.let { v -> AsrMadhab.entries.firstOrNull { it.name == v } } ?: d.prayer.madhab,
            highLatitudeRule = when (val v = m[HIGH_LAT]) {
                null, AUTO -> d.prayer.highLatitudeRule
                else -> HighLatitudeRule.entries.firstOrNull { it.name == v } ?: d.prayer.highLatitudeRule
            },
            mosqueAdjustments = MosqueAdjustments(
                fajr = adj("fajr", da.fajr),
                sunrise = adj("sunrise", da.sunrise),
                dhuhr = adj("dhuhr", da.dhuhr),
                asr = adj("asr", da.asr),
                maghrib = adj("maghrib", da.maghrib),
                isha = adj("isha", da.isha),
            ),
        ),
        bedtime = m[BEDTIME]?.toIntOrNull()?.takeIf { it in 0 until 1440 } ?: d.bedtime,
        qiyamEnabled = m[QIYAM]?.toBooleanStrictOrNull() ?: d.qiyamEnabled,
        roles = m[ROLES]?.let { v ->
            if (v.isEmpty()) emptySet() else v.split(",").mapNotNull { n -> Role.entries.firstOrNull { it.name == n } }.toSet()
        } ?: d.roles,
        blockReminders = m[BLOCK_REMINDERS]?.toBooleanStrictOrNull() ?: d.blockReminders,
        wakeAlarm = m[WAKE_ALARM]?.toBooleanStrictOrNull() ?: d.wakeAlarm,
        theme = m[THEME]?.let { v -> Theme.entries.firstOrNull { it.name == v } } ?: d.theme,
        setupDone = m[SETUP_DONE]?.toBooleanStrictOrNull() ?: d.setupDone,
    )
}
