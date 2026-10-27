package com.dcc.yawm.shared.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Database(
    entities = [SettingEntity::class, TemplateBlockEntity::class, ChecklistMarkEntity::class, DayPageEntity::class],
    version = 1,
)
@ConstructedBy(YawmDatabaseConstructor::class)
abstract class YawmDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun templateBlockDao(): TemplateBlockDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun dayPageDao(): DayPageDao
}

// The Room compiler generates the `actual` for each target.
@Suppress("KotlinNoActualForExpect")
expect object YawmDatabaseConstructor : RoomDatabaseConstructor<YawmDatabase> {
    override fun initialize(): YawmDatabase
}

/** Same SQLite on every phone: the bundled driver. Queries run on Room's default Dispatchers.IO. */
fun RoomDatabase.Builder<YawmDatabase>.buildYawmDatabase(): YawmDatabase =
    setDriver(BundledSQLiteDriver()).build()

const val DATABASE_FILE = "yawm.db"

/** What the app needs from storage, built once from the one database. */
class YawmData(db: YawmDatabase) {
    val settings = SettingsRepository(db.settingDao())
    val dayLog = DayLogRepository(db.checklistDao(), db.dayPageDao())
    val templateBlocks: TemplateBlockDao = db.templateBlockDao()
}
