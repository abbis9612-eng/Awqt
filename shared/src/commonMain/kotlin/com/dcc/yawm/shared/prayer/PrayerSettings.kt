package com.dcc.yawm.shared.prayer

/** A place on Earth, in degrees. Only city coordinates are ever stored (CLAUDE.md rule 5). */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}

/** Calculation methods the user can pick. Our own names, mapped to the engine at the boundary. */
enum class CalcMethod {
    MUSLIM_WORLD_LEAGUE, EGYPTIAN, KARACHI, UMM_AL_QURA, DUBAI, MOON_SIGHTING_COMMITTEE,
    NORTH_AMERICA, KUWAIT, QATAR, SINGAPORE, TURKEY,
}

/** Shafi (and Maliki, Hanbali): shadow equals the object. Hanafi: twice the object. */
enum class AsrMadhab { SHAFI, HANAFI }

/** How Fajr and Isha are bounded where twilight lasts all night. */
enum class HighLatitudeRule { MIDDLE_OF_THE_NIGHT, SEVENTH_OF_THE_NIGHT, TWILIGHT_ANGLE }

/** Minutes added to each computed time so it matches the user's own mosque. */
data class MosqueAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
)

data class PrayerSettings(
    val method: CalcMethod,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    /** null = the engine's recommendation for the latitude. */
    val highLatitudeRule: HighLatitudeRule? = null,
    val mosqueAdjustments: MosqueAdjustments = MosqueAdjustments(),
)
