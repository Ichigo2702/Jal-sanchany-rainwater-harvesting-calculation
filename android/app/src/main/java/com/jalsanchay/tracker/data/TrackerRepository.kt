package com.jalsanchay.tracker.data

import com.jalsanchay.tracker.model.RainfallEntry
import com.jalsanchay.tracker.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackerRepository(
    private val rainfallDao: RainfallDao,
    private val settingsDao: SettingsDao
) {
    val entries: Flow<List<RainfallEntry>> = rainfallDao.observeEntries().map { rows -> rows.map { it.toModel() } }
    val settings: Flow<UserSettings> = settingsDao.observeSettings().map { it?.toModel() ?: UserSettings() }

    suspend fun saveSettings(settings: UserSettings) {
        settingsDao.save(settings.toEntity())
    }

    suspend fun saveEntry(entry: RainfallEntry) {
        if (entry.id == 0L) rainfallDao.insert(entry.toEntity()) else rainfallDao.update(entry.toEntity())
    }

    suspend fun deleteEntry(entry: RainfallEntry) {
        rainfallDao.delete(entry.toEntity())
    }

    suspend fun replaceEntries(entries: List<RainfallEntry>) {
        rainfallDao.clear()
        entries.forEach { rainfallDao.insert(it.copy(id = 0).toEntity()) }
    }

    suspend fun seedIfEmpty(entries: List<RainfallEntry>) {
        if (rainfallDao.count() == 0) {
            rainfallDao.insertAll(entries.map { it.copy(id = 0).toEntity() })
        }
    }

    suspend fun reset() {
        rainfallDao.clear()
        settingsDao.save(UserSettings().toEntity())
    }
}
