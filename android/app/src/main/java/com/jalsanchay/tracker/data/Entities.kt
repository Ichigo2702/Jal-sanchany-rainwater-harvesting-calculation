package com.jalsanchay.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jalsanchay.tracker.model.RainfallEntry
import com.jalsanchay.tracker.model.UserSettings

@Entity(tableName = "rainfall_log")
data class RainfallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val rainfallMm: Double,
    val litresCollected: Double,
    val createdAt: Long
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val setupDone: Boolean = false,
    val roofArea: Double = 800.0,
    val unit: String = "sqft",
    val tankCapacity: Double = 3000.0,
    val runoffCoeff: Double = 0.85,
    val darkMode: Boolean = false,
    val amoledMode: Boolean = false,
    val rainfallReminderEnabled: Boolean = false
)

fun RainfallLogEntity.toModel() = RainfallEntry(id, date, rainfallMm, litresCollected, createdAt)

fun RainfallEntry.toEntity() = RainfallLogEntity(id, date, rainfallMm, litresCollected, createdAt)

fun UserSettingsEntity.toModel() = UserSettings(
    setupDone = setupDone,
    roofArea = roofArea,
    unit = unit,
    tankCapacity = tankCapacity,
    runoffCoeff = runoffCoeff,
    darkMode = darkMode,
    amoledMode = amoledMode,
    rainfallReminderEnabled = rainfallReminderEnabled
)

fun UserSettings.toEntity() = UserSettingsEntity(
    setupDone = setupDone,
    roofArea = roofArea,
    unit = unit,
    tankCapacity = tankCapacity,
    runoffCoeff = runoffCoeff,
    darkMode = darkMode,
    amoledMode = amoledMode,
    rainfallReminderEnabled = rainfallReminderEnabled
)
