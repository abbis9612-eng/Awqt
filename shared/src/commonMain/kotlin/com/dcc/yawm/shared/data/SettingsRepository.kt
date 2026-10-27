package com.dcc.yawm.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: SettingDao) {
    val settings: Flow<UserSettings> = dao.observeAll().map { rows -> decodeSettings(rows.toMap()) }

    suspend fun load(): UserSettings = decodeSettings(dao.all().toMap())

    suspend fun save(settings: UserSettings) {
        dao.upsertAll(encodeSettings(settings).map { (k, v) -> SettingEntity(k, v) })
    }

    private fun List<SettingEntity>.toMap() = associate { it.key to it.value }
}
