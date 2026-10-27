package com.dcc.yawm.shared.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
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

    @Test
    fun blockMarksAreKeptPerDay() = runBlocking {
        val log = YawmData(open()).dayLog
        log.setBlockDone(day, "deep1", true)
        log.setBlockDone(day, "fajr", true)
        log.setBlockDone(day, "fajr", false)
        log.setBlockDone(day.plusDays(1), "nap", true)
        assertEquals(setOf("deep1"), log.blocksDoneOn(day).first())
    }

    /**
     * A phone that ran database version 1 (step 5) keeps its data after the upgrade: build the
     * version-1 file from the committed schema, then let Room migrate it.
     */
    @Test
    fun versionOneDataSurvivesTheUpgrade() = runBlocking {
        val schema = File("schemas/com.dcc.yawm.shared.data.YawmDatabase/1.json").readText()
        val creates = Regex("\"createSql\": \"(.*?)\",\n").findAll(schema).map { m ->
            val sql = m.groupValues[1].replace("\\\"", "\"")
            val table = Regex("\"tableName\": \"(\\w+)\"").findAll(schema.substring(0, m.range.first)).last().groupValues[1]
            sql.replace("\${TABLE_NAME}", table)
        }.toList()
        val setup = Regex("\"setupQueries\": \\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL).find(schema)!!.groupValues[1]
            .let { Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(it).map { q -> q.groupValues[1].replace("\\\"", "\"") }.toList() }
        assertEquals(4, creates.size)
        BundledSQLiteDriver().open(path).apply {
            (creates + setup).forEach { execSQL(it) }
            execSQL("INSERT INTO checklist_mark (date, point, markedAtEpochMs) VALUES ('2026-10-27', 4, 1)")
            execSQL("INSERT INTO setting (key, value) VALUES ('night.bedtime', '1320')")
            execSQL("PRAGMA user_version = 1")
            close()
        }
        val data = YawmData(open())
        assertEquals(setOf(ChecklistPoint.DEEP_WORK), data.dayLog.doneOn(day).first())
        assertEquals(1320, data.settings.load().bedtime)
        data.dayLog.setBlockDone(day, "deep1", true)
        assertEquals(setOf("deep1"), data.dayLog.blocksDoneOn(day).first())
    }

    private fun LocalDate.plusDays(n: Int) = plus(n, DateTimeUnit.DAY)
}
