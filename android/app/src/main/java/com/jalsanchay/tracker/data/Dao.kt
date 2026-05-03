package com.jalsanchay.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSetupDao {
    @Query("SELECT * FROM user_setup WHERE id = 1")
    fun getSetup(): Flow<UserSetup?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetup(setup: UserSetup)
}

@Dao
interface RainfallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: RainfallEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RainfallEntry>)

    @Update
    suspend fun updateEntry(entry: RainfallEntry)

    @Delete
    suspend fun deleteEntry(entry: RainfallEntry)

    @Query("SELECT * FROM rainfall_log ORDER BY date DESC")
    fun getAllEntries(): Flow<List<RainfallEntry>>

    @Query("""
        SELECT * FROM rainfall_log
        WHERE date LIKE :monthPrefix || '%'
        ORDER BY date DESC
    """)
    fun getEntriesByMonth(monthPrefix: String): Flow<List<RainfallEntry>>

    @Query("SELECT * FROM rainfall_log WHERE date = :today")
    fun getTodayEntries(today: String): Flow<List<RainfallEntry>>

    @Query("SELECT COALESCE(SUM(litresCollected), 0.0) FROM rainfall_log")
    fun getTotalLitres(): Flow<Double>

    @Query("SELECT COUNT(*) FROM rainfall_log")
    fun getEntryCount(): Flow<Int>

    @Query("SELECT * FROM rainfall_log WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): RainfallEntry?

    @Query("SELECT * FROM rainfall_log ORDER BY date ASC")
    suspend fun getAllEntriesList(): List<RainfallEntry>

    @Query("DELETE FROM rainfall_log")
    suspend fun clearEntries()
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE id = 1")
    suspend fun getWeatherCache(): WeatherCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeatherCache(cache: WeatherCache)
}
