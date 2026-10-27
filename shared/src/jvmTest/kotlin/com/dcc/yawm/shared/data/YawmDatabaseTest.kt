package com.dcc.yawm.shared.data

import androidx.room3.Room
import com.dcc.yawm.shared.day.ChecklistPoint
import com.dcc.yawm.shared.day.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Real Room with the bundled SQLite, in a file, the same way the phone opens it. */
class YawmDatabaseTest {
    private val dir: File = Files.createTempDirectory("yawm-db").toFile()
    private val path = File(dir, DATABASE_FILE).absolutePath
    private val opened = mutableListOf<YawmDatabase>()

    private fun open(): YawmDatabase =
        Room.databaseBuilder<YawmDatabase>(name = path).buildYawmDatabase().also { opened += it }

    @AfterTest
    fun cleanUp() {
        opened.forEach { it.close() }
        dir.deleteRecursively()
    }

    private val day = LocalDate(2026, 10, 27)

    @Test
    fun settingsStartAtDefaultsAndKeepWhatIsSaved() = runBlocking {
        val data = YawmData(open())
        assertEquals(UserSettings(), data.settings.load())
        val mine = UserSettings(bedtime = 22 * 60, qiyamEnabled = false, roles = setOf(Role.WORSHIP, Role.BODY))
        data.settings.save(mine)
        assertEquals(mine, data.settings.load())
        assertEquals(mine, data.settings.settings.first())
    }

    @Test
    fun checklistMarksAndUnmarksPerDay() = runBlocking {
        val log = YawmData(open()).dayLog
        log.setDone(day, ChecklistPoint.PRAYERS, true)
        log.setDone(day, ChecklistPoint.DEEP_WORK, true)
        log.setDone(day, ChecklistPoint.DEEP_WORK, true) // twice is still once
        log.setDone(day.plusDays(1), ChecklistPoint.NAP, true)
        assertEquals(setOf(ChecklistPoint.PRAYERS, ChecklistPoint.DEEP_WORK), log.doneOn(day).first())

        log.setDone(day, ChecklistPoint.PRAYERS, false)
        assertEquals(setOf(ChecklistPoint.DEEP_WORK), log.doneOn(day).first())
        assertEquals(
            mapOf(day to setOf(ChecklistPoint.DEEP_WORK), day.plusDays(1) to setOf(ChecklistPoint.NAP)),
            log.doneBetween(day.plusDays(-6), day.plusDays(1)),
        )
        assertEquals(emptyMap(), log.doneBetween(day.plusDays(2), day.plusDays(8)))
    }

    @Test
    fun dayPageIsSavedAndReplaced() = runBlocking {
        val log = YawmData(open()).dayLog
        assertNull(log.page(day))
        log.savePage(day, DayPage("صليت وقرأت", "التلفون بعد العصر", "1، 2، 3"))
        log.savePage(day, DayPage("صليت وقرأت ومشيت", "التلفون بعد العصر", "1، 2، 3"))
        assertEquals(DayPage("صليت وقرأت ومشيت", "التلفون بعد العصر", "1، 2، 3"), log.page(day))
        assertEquals(log.page(day), log.observePage(day).first())
    }

    @Test
    fun everythingIsStillThereAfterClosingAndReopening() = runBlocking {
        val first = open()
        YawmData(first).apply {
            settings.save(UserSettings(cityName = "أربيل", bedtime = 1290))
            dayLog.setDone(day, ChecklistPoint.SLEEP_ON_TIME, true)
            dayLog.savePage(day, DayPage("a", "b", "c"))
            templateBlocks.upsert(TemplateBlockEntity("WORKDAY", "walk", preferredMinutes = 25, hidden = false))
        }
        first.close()

        val again = YawmData(open())
        assertEquals(UserSettings(cityName = "أربيل", bedtime = 1290), again.settings.load())
        assertEquals(setOf(ChecklistPoint.SLEEP_ON_TIME), again.dayLog.doneOn(day).first())
        assertEquals(DayPage("a", "b", "c"), again.dayLog.page(day))
        assertEquals(
            listOf(TemplateBlockEntity("WORKDAY", "walk", 25, false)),
            again.templateBlocks.forDayType("WORKDAY"),
        )
    }

    private fun LocalDate.plusDays(n: Int) = plus(n, DateTimeUnit.DAY)
}
