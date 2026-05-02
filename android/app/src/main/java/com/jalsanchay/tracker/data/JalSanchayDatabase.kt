package com.jalsanchay.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RainfallLogEntity::class, UserSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JalSanchayDatabase : RoomDatabase() {
    abstract fun rainfallDao(): RainfallDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var instance: JalSanchayDatabase? = null

        fun getDatabase(context: Context): JalSanchayDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JalSanchayDatabase::class.java,
                    "jal_sanchay.db"
                ).build().also { instance = it }
            }
        }
    }
}
