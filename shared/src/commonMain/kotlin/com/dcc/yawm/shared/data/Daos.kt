package com.dcc.yawm.shared.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT * FROM setting")
    fun observeAll(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM setting")
    suspend fun all(): List<SettingEntity>

    @Upsert
    suspend fun upsertAll(rows: List<SettingEntity>)
}

@Dao
interface TemplateBlockDao {
    @Query("SELECT * FROM template_block WHERE dayType = :dayType")
    suspend fun forDayType(dayType: String): List<TemplateBlockEntity>

    @Upsert
    suspend fun upsert(row: TemplateBlockEntity)

    @Query("DELETE FROM template_block WHERE dayType = :dayType AND blockKey = :blockKey")
    suspend fun reset(dayType: String, blockKey: String)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_mark WHERE date = :date")
    fun observeDay(date: String): Flow<List<ChecklistMarkEntity>>

    @Query("SELECT * FROM checklist_mark WHERE date BETWEEN :from AND :to ORDER BY date, point")
    suspend fun between(from: String, to: String): List<ChecklistMarkEntity>

    @Upsert
    suspend fun mark(row: ChecklistMarkEntity)

    @Query("DELETE FROM checklist_mark WHERE date = :date AND point = :point")
    suspend fun unmark(date: String, point: Int)
}

@Dao
interface DayPageDao {
    @Query("SELECT * FROM day_page WHERE date = :date")
    suspend fun get(date: String): DayPageEntity?

    @Query("SELECT * FROM day_page WHERE date = :date")
    fun observe(date: String): Flow<DayPageEntity?>

    @Upsert
    suspend fun save(page: DayPageEntity)
}

@Dao
interface BlockDoneDao {
    @Query("SELECT blockKey FROM block_done WHERE date = :date")
    fun observeDay(date: String): Flow<List<String>>

    @Upsert
    suspend fun mark(row: BlockDoneEntity)

    @Query("DELETE FROM block_done WHERE date = :date AND blockKey = :blockKey")
    suspend fun unmark(date: String, blockKey: String)
}
