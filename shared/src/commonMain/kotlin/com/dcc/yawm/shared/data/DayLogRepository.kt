package com.dcc.yawm.shared.data

import com.dcc.yawm.shared.day.ChecklistPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The three answers of the evening review (docs/METHOD.md section 7). */
data class DayPage(
    val didToday: String = "",
    val lostTime: String = "",
    val tomorrowTopThree: String = "",
)

/** The day log (ten points) and the day page, keyed by the date in the city's zone. */
class DayLogRepository(
    private val checklist: ChecklistDao,
    private val pages: DayPageDao,
    private val nowEpochMs: () -> Long = ::systemNowEpochMs,
) {
    fun doneOn(date: LocalDate): Flow<Set<ChecklistPoint>> =
        checklist.observeDay(date.toString()).map { rows -> rows.mapNotNull { ChecklistPoint.ofNumber(it.point) }.toSet() }

    suspend fun setDone(date: LocalDate, point: ChecklistPoint, done: Boolean) {
        if (done) checklist.mark(ChecklistMarkEntity(date.toString(), point.number, nowEpochMs()))
        else checklist.unmark(date.toString(), point.number)
    }

    /** Points done on each date from [from] to [to], both included; dates with none are absent. */
    suspend fun doneBetween(from: LocalDate, to: LocalDate): Map<LocalDate, Set<ChecklistPoint>> =
        checklist.between(from.toString(), to.toString())
            .groupBy { LocalDate.parse(it.date) }
            .mapValues { (_, rows) -> rows.mapNotNull { ChecklistPoint.ofNumber(it.point) }.toSet() }

    suspend fun page(date: LocalDate): DayPage? = pages.get(date.toString())?.toPage()

    fun observePage(date: LocalDate): Flow<DayPage?> = pages.observe(date.toString()).map { it?.toPage() }

    suspend fun savePage(date: LocalDate, page: DayPage) {
        pages.save(DayPageEntity(date.toString(), page.didToday, page.lostTime, page.tomorrowTopThree, nowEpochMs()))
    }

    private fun DayPageEntity.toPage() = DayPage(didToday, lostTime, tomorrowTopThree)
}

@OptIn(ExperimentalTime::class)
private fun systemNowEpochMs(): Long = Clock.System.now().toEpochMilliseconds()
