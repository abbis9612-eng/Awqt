package com.dcc.yawm.shared.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// The four tables of step 5 (docs/PRODUCT.md). Dates are ISO strings ("2026-10-27") in the
// city's zone, so they sort and compare as text.

/** One user setting. Typed access goes through [SettingsRepository]. */
@Entity(tableName = "setting")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/**
 * The user's change to one block of a day template. Which changes the user may make is still
 * open in docs/DECISIONS.md, so the resolver does not read this table yet.
 */
@Entity(tableName = "template_block", primaryKeys = ["dayType", "blockKey"])
data class TemplateBlockEntity(
    val dayType: String,
    val blockKey: String,
    /** null = keep the template's preferred minutes. */
    val preferredMinutes: Int?,
    val hidden: Boolean,
)

/** Day log: a row means that point of the ten was done on that date. */
@Entity(tableName = "checklist_mark", primaryKeys = ["date", "point"])
data class ChecklistMarkEntity(
    val date: String,
    /** 1 to 10, as numbered in docs/METHOD.md section 6. */
    val point: Int,
    val markedAtEpochMs: Long,
)

/** Day page (المحاسبة): the answers to the three evening questions of docs/METHOD.md section 7. */
@Entity(tableName = "day_page")
data class DayPageEntity(
    @PrimaryKey val date: String,
    val didToday: String,
    val lostTime: String,
    val tomorrowTopThree: String,
    val savedAtEpochMs: Long,
)

/** A block of the day the user marked done («خلّصت», «صلّيت», «صحيت»). Added in database version 2. */
@Entity(tableName = "block_done", primaryKeys = ["date", "blockKey"])
data class BlockDoneEntity(
    val date: String,
    val blockKey: String,
    val doneAtEpochMs: Long,
)
