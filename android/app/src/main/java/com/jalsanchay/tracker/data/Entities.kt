package com.jalsanchay.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_setup")
data class UserSetup(
    @PrimaryKey
    val id: Int = 1,
    val roofArea: Double = 800.0,
    val unit: String = "sqft",
    val tankCapacity: Double = 3000.0,
    val runoffCoefficient: Double = 0.85,
    val runoffLabel: String = "Concrete",
    val locationName: String = "",
    val setupDone: Boolean = false,
    val runoffCoeff: Double = runoffCoefficient,
    val darkMode: Boolean = false,
    val amoledMode: Boolean = false,
    val rainfallReminderEnabled: Boolean = false
)

@Entity(tableName = "rainfall_log")
data class RainfallEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val rainfallMm: Double,
    val litresCollected: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val id: Int = 1,
    val locationName: String,
    val forecastJson: String,
    val fetchedAt: Long
)
