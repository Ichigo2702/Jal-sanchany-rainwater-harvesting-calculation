package com.jalsanchay.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserSetup::class, RainfallEntry::class, WeatherCache::class],
    version = 2,
    exportSchema = false
)
abstract class JalSanchayDatabase : RoomDatabase() {

    abstract fun userSetupDao(): UserSetupDao
    abstract fun rainfallDao(): RainfallDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: JalSanchayDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_setup ADD COLUMN setupDone INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_setup ADD COLUMN runoffCoeff REAL NOT NULL DEFAULT 0.85")
                database.execSQL("ALTER TABLE user_setup ADD COLUMN darkMode INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_setup ADD COLUMN amoledMode INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_setup ADD COLUMN rainfallReminderEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS weather_cache (
                        id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        locationName TEXT NOT NULL,
                        forecastJson TEXT NOT NULL,
                        fetchedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): JalSanchayDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    JalSanchayDatabase::class.java,
                    "jalsanchay_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
