package com.jalsanchay.tracker.model

import com.jalsanchay.tracker.data.RainfallEntry as DbRainfallEntry
import com.jalsanchay.tracker.data.UserSetup
import com.jalsanchay.tracker.util.Calculations
import com.jalsanchay.tracker.util.ForecastData
import com.jalsanchay.tracker.util.RainDay

typealias UserSettings = UserSetup
typealias RainfallEntry = DbRainfallEntry
typealias MonthlyData = Calculations.MonthlyTotal
typealias MonthlyReport = Calculations.MonthlyTotal

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

sealed class AsyncState<out T> {
    data object Idle : AsyncState<Nothing>()
    data object Loading : AsyncState<Nothing>()
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val message: String) : AsyncState<Nothing>()
}

data class TrackerUiState(
    val settings: UserSetup = UserSetup(),
    val setupComplete: Boolean = false,
    val entries: List<RainfallEntry> = emptyList(),
    val todaySaved: Double = 0.0,
    val monthSaved: Double = 0.0,
    val allTimeSaved: Double = 0.0,
    val impactScore: Double = 0.0,
    val tankPercentage: Float = 0f,
    val streakDays: Int = 0,
    val bestDayLitres: Double = 0.0,
    val avgMonthlyLitres: Double = 0.0,
    val dryDaysCount: Int = 0,
    val currentMonthProgress: Float = 0f,
    val lastLoggedDate: String? = null,
    val litresToFill: Double = 0.0,
    val monthlyTotals: List<Calculations.MonthlyTotal> = emptyList(),
    val monthlyReports: List<Calculations.MonthlyTotal> = emptyList(),
    val bestMonth: Pair<String, Double>? = null,
    val forecast: ForecastData? = null,
    val weatherCacheAge: Long? = null,
    val nextRainDay: RainDay? = null,
    val currentMilestone: Double? = null,
    val isOnline: Boolean = true,
    val aiTipsState: AsyncState<String> = AsyncState.Idle,
    val aiInsightState: AsyncState<String> = AsyncState.Idle,
    val weatherLoadState: AsyncState<Unit> = AsyncState.Idle,
    val isLoading: Boolean = false,
    val milestoneMessage: String? = null,
    val aiTip: String? = null
)
