package com.dcc.yawm.shared.prayer

import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.Coordinates as AdhanCoordinates
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.Madhab as AdhanMadhab
import com.batoulapps.adhan2.PrayerAdjustments as AdhanAdjustments
import com.batoulapps.adhan2.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

enum class Prayer { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

/** The six times of one day in one city, as absolute instants (CLAUDE.md, time model). */
data class DayPrayerTimes(
    val date: LocalDate,
    val zone: TimeZone,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
) {
    operator fun get(prayer: Prayer): Instant = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    /**
     * Wall-clock minutes from this date's local midnight; above 1440 when the time falls on the next
     * day (Isha after midnight in a London summer). This is the reference data's unit.
     */
    fun wallMinutes(prayer: Prayer): Int {
        val local = this[prayer].toLocalDateTime(zone)
        return date.daysUntil(local.date) * 1440 + local.hour * 60 + local.minute
    }
}

/**
 * The only place that talks to the adhan library (pre-1.0, pinned): everything outside uses our types.
 */
object PrayerTimesEngine {
    fun compute(place: GeoPoint, date: LocalDate, zone: TimeZone, settings: PrayerSettings): DayPrayerTimes {
        val a = settings.mosqueAdjustments
        val params = settings.method.toAdhan().parameters.copy(
            madhab = when (settings.madhab) {
                AsrMadhab.SHAFI -> AdhanMadhab.SHAFI
                AsrMadhab.HANAFI -> AdhanMadhab.HANAFI
            },
            highLatitudeRule = settings.highLatitudeRule?.toAdhan(),
            prayerAdjustments = AdhanAdjustments(a.fajr, a.sunrise, a.dhuhr, a.asr, a.maghrib, a.isha),
        )
        val t = AdhanPrayerTimes(
            AdhanCoordinates(place.latitude, place.longitude),
            DateComponents(date.year, date.month.ordinal + 1, date.day),
            params,
        )
        return DayPrayerTimes(date, zone, t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
    }

    private fun CalcMethod.toAdhan(): AdhanMethod = when (this) {
        CalcMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE
        CalcMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN
        CalcMethod.KARACHI -> AdhanMethod.KARACHI
        CalcMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA
        CalcMethod.DUBAI -> AdhanMethod.DUBAI
        CalcMethod.MOON_SIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE
        CalcMethod.NORTH_AMERICA -> AdhanMethod.NORTH_AMERICA
        CalcMethod.KUWAIT -> AdhanMethod.KUWAIT
        CalcMethod.QATAR -> AdhanMethod.QATAR
        CalcMethod.SINGAPORE -> AdhanMethod.SINGAPORE
        CalcMethod.TURKEY -> AdhanMethod.TURKEY
    }

    private fun HighLatitudeRule.toAdhan(): AdhanHighLatitudeRule = when (this) {
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
        HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
    }
}
