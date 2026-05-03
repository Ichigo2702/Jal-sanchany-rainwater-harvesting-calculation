package com.jalsanchay.tracker.data

import com.jalsanchay.tracker.util.Calculations
import com.jalsanchay.tracker.ai.AiTipService
import kotlinx.coroutines.flow.Flow

class TrackerRepository(private val db: JalSanchayDatabase) {
    val WEATHER_CACHE_TTL_MS = 3 * 60 * 60 * 1000L

    val setup: Flow<UserSetup?> = db.userSetupDao().getSetup()

    suspend fun saveSetup(setup: UserSetup) =
        db.userSetupDao().saveSetup(setup)

    val allEntries: Flow<List<RainfallEntry>> =
        db.rainfallDao().getAllEntries()

    val totalLitres: Flow<Double> =
        db.rainfallDao().getTotalLitres()

    val entryCount: Flow<Int> =
        db.rainfallDao().getEntryCount()

    fun getTodayEntries(today: String): Flow<List<RainfallEntry>> =
        db.rainfallDao().getTodayEntries(today)

    fun getEntriesByMonth(monthPrefix: String): Flow<List<RainfallEntry>> =
        db.rainfallDao().getEntriesByMonth(monthPrefix)

    suspend fun insertEntry(entry: RainfallEntry) {
        db.rainfallDao().insertEntry(entry)
        AiTipService.invalidateEntryRelatedCache()
    }

    suspend fun updateEntry(entry: RainfallEntry) {
        db.rainfallDao().updateEntry(entry)
        AiTipService.invalidateEntryRelatedCache()
    }

    suspend fun deleteEntry(entry: RainfallEntry) {
        db.rainfallDao().deleteEntry(entry)
        AiTipService.invalidateEntryRelatedCache()
    }

    suspend fun insertAll(entries: List<RainfallEntry>) =
        db.rainfallDao().insertAll(entries)

    suspend fun replaceEntries(entries: List<RainfallEntry>) {
        db.rainfallDao().clearEntries()
        insertAll(entries)
        AiTipService.invalidateEntryRelatedCache()
    }

    suspend fun getAllEntriesList(): List<RainfallEntry> =
        db.rainfallDao().getAllEntriesList()

    suspend fun getEntryByDate(date: String): RainfallEntry? =
        db.rainfallDao().getEntryByDate(date)

    suspend fun getWeatherCache(): WeatherCache? {
        val cache = db.weatherDao().getWeatherCache() ?: return null
        val age = System.currentTimeMillis() - cache.fetchedAt
        return if (age < WEATHER_CACHE_TTL_MS) cache else null
    }

    suspend fun saveWeatherCache(locationName: String, forecastJson: String) {
        db.weatherDao().saveWeatherCache(
            WeatherCache(
                locationName = locationName,
                forecastJson = forecastJson,
                fetchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recalculateAllEntries(setup: UserSetup) {
        val entries = getAllEntriesList()
        entries.forEach { entry ->
            val newLitres = Calculations.calculateWaterCollected(
                roofArea = setup.roofArea,
                unit = setup.unit,
                rainfallMm = entry.rainfallMm,
                runoffCoeff = setup.runoffCoefficient,
                tankCapacity = setup.tankCapacity
            )
            updateEntry(entry.copy(litresCollected = newLitres))
        }
    }

    suspend fun seedIfEmpty(entries: List<RainfallEntry>, setup: UserSetup) {
        val count = db.rainfallDao().getAllEntriesList().size
        if (count == 0) {
            saveSetup(setup)
            insertAll(entries)
        }
    }
}
