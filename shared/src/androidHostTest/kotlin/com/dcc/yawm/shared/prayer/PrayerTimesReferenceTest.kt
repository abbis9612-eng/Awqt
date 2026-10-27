package com.dcc.yawm.shared.prayer

import com.dcc.yawm.shared.ReferenceCase
import com.dcc.yawm.shared.ReferenceData
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** CLAUDE.md: every day of reference-2026.csv, all three cases, within ±2 minutes. */
class PrayerTimesReferenceTest {
    private val tolerance = 2

    @Test
    fun everyReferenceDayMatchesWithinTwoMinutes() {
        val rows = ReferenceData.rows
        assertEquals(1095, rows.size, "reference rows")
        val failures = mutableListOf<String>()
        val worst = mutableMapOf<String, Int>()
        for (r in rows) {
            val t = PrayerTimesEngine.compute(r.case.place, r.date, r.case.zone, r.case.settings)
            val expected = listOf(
                Prayer.FAJR to r.fajr, Prayer.SUNRISE to r.sunrise, Prayer.DHUHR to r.dhuhr,
                Prayer.ASR to r.asr, Prayer.MAGHRIB to r.maghrib, Prayer.ISHA to r.isha,
            )
            for ((p, want) in expected) {
                val diff = abs(t.wallMinutes(p) - want)
                val key = "${r.case.id} $p"
                worst[key] = maxOf(worst[key] ?: 0, diff)
                if (diff > tolerance) failures += "${r.case.id} ${r.date} $p: got ${t.wallMinutes(p)}, want $want"
            }
        }
        println("Largest difference per case and prayer (minutes): " + worst.entries.joinToString { "${it.key}=${it.value}" })
        assertTrue(failures.isEmpty(), "${failures.size} times off by more than $tolerance min:\n" + failures.take(20).joinToString("\n"))
    }

    @Test
    fun mosqueAdjustmentsShiftOnlyTheirPrayer() {
        val c = ReferenceCase.SULAYMANIYAH_SHAFI
        val date = kotlinx.datetime.LocalDate(2026, 10, 15)
        val base = PrayerTimesEngine.compute(c.place, date, c.zone, c.settings)
        val moved = PrayerTimesEngine.compute(
            c.place, date, c.zone, c.settings.copy(mosqueAdjustments = MosqueAdjustments(fajr = 3, isha = -2)),
        )
        assertEquals(base.wallMinutes(Prayer.FAJR) + 3, moved.wallMinutes(Prayer.FAJR))
        assertEquals(base.wallMinutes(Prayer.ISHA) - 2, moved.wallMinutes(Prayer.ISHA))
        for (p in listOf(Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB)) {
            assertEquals(base.wallMinutes(p), moved.wallMinutes(p), "$p must not move")
        }
    }
}
