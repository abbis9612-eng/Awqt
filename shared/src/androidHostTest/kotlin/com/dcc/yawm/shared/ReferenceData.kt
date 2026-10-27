package com.dcc.yawm.shared

import com.dcc.yawm.shared.prayer.AsrMadhab
import com.dcc.yawm.shared.prayer.CalcMethod
import com.dcc.yawm.shared.prayer.GeoPoint
import com.dcc.yawm.shared.prayer.HighLatitudeRule
import com.dcc.yawm.shared.prayer.PrayerSettings
import java.io.File
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The three cases of docs/reference/resolver_reference.py (CASES), with the same parameters. */
enum class ReferenceCase(val id: String, val place: GeoPoint, val zone: TimeZone, val settings: PrayerSettings) {
    SULAYMANIYAH_SHAFI(
        "sulaymaniyah-shafi", GeoPoint(35.5613, 45.4375), TimeZone.of("Asia/Baghdad"),
        PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI),
    ),
    SULAYMANIYAH_HANAFI(
        "sulaymaniyah-hanafi", GeoPoint(35.5613, 45.4375), TimeZone.of("Asia/Baghdad"),
        PrayerSettings(CalcMethod.EGYPTIAN, AsrMadhab.HANAFI),
    ),
    LONDON_SHAFI(
        "london-shafi", GeoPoint(51.5074, -0.1278), TimeZone.of("Europe/London"),
        PrayerSettings(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI, HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
    );

    companion object {
        fun byId(id: String) = entries.first { it.id == id }
    }
}

/** One row of docs/reference/reference-2026.csv; times are wall-clock minutes from local midnight. */
data class ReferenceRow(
    val case: ReferenceCase,
    val date: LocalDate,
    val fajr: Int, val sunrise: Int, val dhuhr: Int, val asr: Int, val maghrib: Int, val isha: Int,
    val bedtime: Int, val wake: Int, val qiyam: Boolean,
    val nightSleep: Int, val nap: Int, val totalSleep: Int, val shortfall: Int,
)

object ReferenceData {
    /** Finds docs/reference from the test's working directory (the module) or any parent. */
    fun file(name: String): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val f = File(dir, "docs/reference/$name")
            if (f.exists()) return f
            dir = dir.parentFile
        }
        error("docs/reference/$name not found above ${File("").absolutePath}")
    }

    /** "05:29" -> 329, "00:12+1" -> 1452 */
    fun minutes(hhmm: String): Int {
        val next = hhmm.endsWith("+1")
        val (h, m) = hhmm.removeSuffix("+1").split(":").map { it.toInt() }
        return h * 60 + m + if (next) 1440 else 0
    }

    val rows: List<ReferenceRow> by lazy {
        file("reference-2026.csv").readLines().map { it.trim() }.filter { it.isNotEmpty() }.drop(1).map { line ->
            val c = line.split(",")
            ReferenceRow(
                ReferenceCase.byId(c[0]), LocalDate.parse(c[1]),
                minutes(c[2]), minutes(c[3]), minutes(c[4]), minutes(c[5]), minutes(c[6]), minutes(c[7]),
                minutes(c[8]), minutes(c[9]), c[10] == "1",
                c[11].toInt(), c[12].toInt(), c[13].toInt(), c[14].toInt(),
            )
        }
    }
}
