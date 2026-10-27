package com.dcc.yawm.shared.day

import com.dcc.yawm.shared.ReferenceCase
import com.dcc.yawm.shared.ReferenceData
import com.dcc.yawm.shared.ReferenceRow
import com.dcc.yawm.shared.prayer.Prayer
import com.dcc.yawm.shared.prayer.PrayerTimesEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Steps 3 and 4: the Kotlin port must reproduce docs/reference exactly when given the same prayer
 * times, and every day of 2026 must satisfy the invariants in all three cases.
 */
class DayReferenceTest {
    private val byKey = ReferenceData.rows.associateBy { it.case to it.date }

    private fun minutesOf(r: ReferenceRow) = DayMinutes(r.fajr, r.sunrise, r.dhuhr, r.asr, r.maghrib, r.isha)

    /** The neighbour day's Fajr or Isha from the CSV; outside 2026, from the engine. */
    private fun fajrOf(case: ReferenceCase, date: LocalDate): Int =
        byKey[case to date]?.fajr ?: PrayerTimesEngine.compute(case.place, date, case.zone, case.settings).wallMinutes(Prayer.FAJR)

    private fun ishaOf(case: ReferenceCase, date: LocalDate): Int =
        byKey[case to date]?.isha ?: PrayerTimesEngine.compute(case.place, date, case.zone, case.settings).wallMinutes(Prayer.ISHA)

    private fun resolveFromCsv(r: ReferenceRow): ResolvedDay = DayResolver.resolve(
        r.date, minutesOf(r),
        nextFajr = fajrOf(r.case, r.date.plus(1, DateTimeUnit.DAY)) + 1440,
        prevIsha = ishaOf(r.case, r.date.minus(1, DateTimeUnit.DAY)),
    )

    @Test
    fun nightBalanceMatchesEveryReferenceRow() {
        val failures = mutableListOf<String>()
        val shortfallDays = mutableMapOf<String, Int>()
        for (r in ReferenceData.rows) {
            val p = resolveFromCsv(r).plan
            val got = listOf(p.bedtime % 1440, p.wake % 1440, p.qiyam, p.nightSleep, p.nap, p.total, p.shortfall)
            val want = listOf(r.bedtime % 1440, r.wake % 1440, r.qiyam, r.nightSleep, r.nap, r.totalSleep, r.shortfall)
            if (got != want) failures += "${r.case.id} ${r.date}: got $got want $want"
            if (p.shortfall > 0) shortfallDays[r.case.id] = (shortfallDays[r.case.id] ?: 0) + 1
        }
        println("Days where 7 h cannot be reached: $shortfallDays")
        assertTrue(failures.isEmpty(), "${failures.size} night plans differ:\n" + failures.take(20).joinToString("\n"))
    }

    @Test
    fun productDocExamples() {
        val oct15 = resolveFromCsv(byKey.getValue(ReferenceCase.SULAYMANIYAH_SHAFI to LocalDate(2026, 10, 15))).plan
        assertEquals(listOf(21 * 60, 4 * 60 + 13, true, 7 * 60 + 13, 20), listOf(oct15.bedtime, oct15.wake % 1440, oct15.qiyam, oct15.nightSleep, oct15.nap))
        val jun15 = resolveFromCsv(byKey.getValue(ReferenceCase.SULAYMANIYAH_SHAFI to LocalDate(2026, 6, 15))).plan
        assertEquals(listOf(21 * 60 + 30, 2 * 60 + 33, 5 * 60 + 3, 90, 27), listOf(jun15.bedtime, jun15.wake % 1440, jun15.nightSleep, jun15.nap, jun15.shortfall))
    }

    @Test
    fun sampleDaysMatchBlockByBlock() {
        val samples = SampleDays.load()
        assertEquals(8, samples.size, "sample days")
        val failures = mutableListOf<String>()
        for (s in samples) {
            val r = byKey.getValue(ReferenceCase.byId(s.caseId) to s.date)
            val day = resolveFromCsv(r)
            val got = day.blocks.map { Triple(it.key, it.start.mod(1440), it.end.mod(1440)) }
            if (got != s.blocks) failures += "${s.caseId} ${s.date} blocks:\n got  $got\n want ${s.blocks}"
            val notes = day.adjustments.map { it.toString() }
            if (notes != s.notes) failures += "${s.caseId} ${s.date} notes: got $notes want ${s.notes}"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun invariantsHoldOnEveryDayWithReferenceTimes() {
        val failures = ReferenceData.rows.flatMap { r -> checkInvariants(resolveFromCsv(r)).map { "${r.case.id} ${r.date}: $it" } }
        assertTrue(failures.isEmpty(), "${failures.size} failures:\n" + failures.take(20).joinToString("\n"))
    }

    @Test
    fun invariantsHoldOnEveryDayWithEngineTimes() {
        val failures = mutableListOf<String>()
        for (case in ReferenceCase.entries) {
            var date = LocalDate(2026, 1, 1)
            repeat(365) {
                val day = DayResolver.resolve(
                    PrayerTimesEngine.compute(case.place, date.minus(1, DateTimeUnit.DAY), case.zone, case.settings),
                    PrayerTimesEngine.compute(case.place, date, case.zone, case.settings),
                    PrayerTimesEngine.compute(case.place, date.plus(1, DateTimeUnit.DAY), case.zone, case.settings),
                )
                checkInvariants(day).forEach { failures += "${case.id} $date: $it" }
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
        assertTrue(failures.isEmpty(), "${failures.size} failures:\n" + failures.take(20).joinToString("\n"))
    }
}

/** Reads docs/reference/sample-days.json (pretty-printed, one field per line) without a JSON library. */
object SampleDays {
    data class Sample(val caseId: String, val date: LocalDate, val notes: List<String>, val blocks: List<Triple<String, Int, Int>>)

    private val keyLine = Regex("""^ "([a-z-]+):(\d{4}-\d{2}-\d{2})": \{$""")
    private val field = Regex("""^\s*"(key|start|end)": "([^"]*)",?$""")
    private val note = Regex("""^\s*"((?:shrunk|dropped):[a-z_0-9]+|overflow)",?$""")

    fun load(): List<Sample> {
        val out = mutableListOf<Sample>()
        var caseId = ""
        var date: LocalDate? = null
        var notes = mutableListOf<String>()
        var blocks = mutableListOf<Triple<String, Int, Int>>()
        var key = ""
        var start = 0
        fun flush() { date?.let { out += Sample(caseId, it, notes, blocks) } }
        for (line in ReferenceData.file("sample-days.json").readLines()) {
            keyLine.find(line)?.let { m ->
                flush()
                caseId = m.groupValues[1]; date = LocalDate.parse(m.groupValues[2])
                notes = mutableListOf(); blocks = mutableListOf()
                return@let
            }
            note.find(line)?.let { notes += it.groupValues[1] }
            field.find(line)?.let { m ->
                when (m.groupValues[1]) {
                    "key" -> key = m.groupValues[2]
                    "start" -> start = ReferenceData.minutes(m.groupValues[2])
                    "end" -> blocks += Triple(key, start, ReferenceData.minutes(m.groupValues[2]))
                }
            }
        }
        flush()
        return out
    }
}
