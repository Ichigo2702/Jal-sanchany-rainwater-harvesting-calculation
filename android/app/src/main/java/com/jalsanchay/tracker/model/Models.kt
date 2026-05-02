package com.jalsanchay.tracker.model

data class RainfallEntry(
    val id: Long = 0,
    val date: String,
    val rainfallMm: Double,
    val litresCollected: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserSettings(
    val setupDone: Boolean = false,
    val roofArea: Double = 800.0,
    val unit: String = "sqft",
    val tankCapacity: Double = 3000.0,
    val runoffCoeff: Double = 0.85,
    val darkMode: Boolean = false,
    val amoledMode: Boolean = false,
    val rainfallReminderEnabled: Boolean = false
)

data class MonthlyReport(
    val monthKey: String,
    val totalRainfallMm: Double,
    val totalWaterSaved: Double,
    val impactDays: Double
)

typealias MonthlyData = MonthlyReport

data class ForecastDay(
    val date: String,
    val precipitationSum: Double
)

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val value: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

data class TrackerUiState(
    val settings: UserSettings = UserSettings(),
    val entries: List<RainfallEntry> = emptyList(),
    val monthlyReports: List<MonthlyReport> = emptyList(),
    val isLoading: Boolean = true,
    val milestoneMessage: String? = null,
    val aiTip: String? = null
)
