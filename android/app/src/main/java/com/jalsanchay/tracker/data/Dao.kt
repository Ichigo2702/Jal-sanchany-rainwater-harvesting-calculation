package com.jalsanchay.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RainfallDao {
    @Query("SELECT * FROM rainfall_log ORDER BY date DESC, createdAt DESC")
    fun observeEntries(): Flow<List<RainfallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RainfallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RainfallLogEntity>)

    @Update
    suspend fun update(entry: RainfallLogEntity)

    @Delete
    suspend fun delete(entry: RainfallLogEntity)

    @Query("DELETE FROM rainfall_log")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM rainfall_log")
    suspend fun count(): Int
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: UserSettingsEntity)
}
