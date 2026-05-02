package com.jalsanchay.tracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jalsanchay.tracker.ai.AiTipService
import com.jalsanchay.tracker.data.TrackerRepository
import com.jalsanchay.tracker.model.RainfallEntry
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.model.UiState
import com.jalsanchay.tracker.model.UserSettings
import com.jalsanchay.tracker.notifications.ReminderScheduler
import com.jalsanchay.tracker.util.PdfExporter
import com.jalsanchay.tracker.util.buildMonthlyReports
import com.jalsanchay.tracker.util.calculateWaterCollected
import com.jalsanchay.tracker.util.recalculateEntries
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class TrackerViewModel(
    private val repository: TrackerRepository,
    context: Context
) : ViewModel() {
    private val pdfExporter = PdfExporter(context)
    private val reminderScheduler = ReminderScheduler(context)
    private val aiTipService = AiTipService()
    private var previousTotal = 0.0
    private val dismissedMilestones = MutableStateFlow<Set<Double>>(emptySet())

    val messages = MutableSharedFlow<String>()
    val locationName = MutableStateFlow("")
    val aiSeasonInsight = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiTips = MutableStateFlow<UiState<String>>(UiState.Idle)
    val glossaryAnswer = MutableSharedFlow<Pair<String, String>>()

    val uiState = combine(repository.settings, repository.entries) { settings, entries ->
        val total = entries.sumOf { it.litresCollected }
        val milestone = milestoneMessage(previousTotal, total)
        previousTotal = total
        TrackerUiState(
            settings = settings,
            entries = entries,
            monthlyReports = buildMonthlyReports(entries),
            isLoading = false,
            milestoneMessage = milestone
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState())

    val streakDays: StateFlow<Int> = repository.entries.map { entries ->
        countStreak(entries.map { it.date }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val bestDayLitres: StateFlow<Double> = repository.entries.map { entries ->
        entries.maxOfOrNull { it.litresCollected } ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val avgMonthlyLitres: StateFlow<Double> = repository.entries.map { entries ->
        val months = entries.map { it.date.take(7) }.distinct().size
        if (months == 0) 0.0 else entries.sumOf { it.litresCollected } / months
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val dryDaysCount: StateFlow<Int> = repository.entries.map { entries ->
        entries.count { it.rainfallMm == 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentMonthProgress: StateFlow<Float> = repository.entries.map { entries ->
        val months = entries.map { it.date.take(7) }.distinct().size
        if (months < 2) 0f else {
            val currentMonth = YearMonth.now().toString()
            val total = entries.sumOf { it.litresCollected }
            val average = total / months
            val current = entries.filter { it.date.startsWith(currentMonth) }.sumOf { it.litresCollected }
            if (average <= 0.0) 0f else (current / average).toFloat().coerceIn(0f, 1f)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val distinctMonthCount: StateFlow<Int> = repository.entries.map { entries ->
        entries.map { it.date.take(7) }.distinct().size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val bestMonth: StateFlow<Pair<String, Double>?> = repository.entries.map { entries ->
        buildMonthlyReports(entries).maxByOrNull { it.totalWaterSaved }?.let { it.monthKey to it.totalWaterSaved }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val previousMonthLitres: StateFlow<Double> = repository.entries.map { entries ->
        buildMonthlyReports(entries).sortedBy { it.monthKey }.dropLast(1).lastOrNull()?.totalWaterSaved ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val currentMilestone: StateFlow<Double?> = combine(repository.entries, dismissedMilestones) { entries, dismissed ->
        val total = entries.sumOf { it.litresCollected }
        listOf(500.0, 1000.0, 5000.0, 10000.0).filter { total >= it && it !in dismissed }.maxOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val forecastDays: StateFlow<List<com.jalsanchay.tracker.model.ForecastDay>> = locationName.map { location ->
        if (location.isBlank()) emptyList() else listOf(
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().toString(), 0.0),
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().plusDays(1).toString(), 8.0),
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().plusDays(2).toString(), 2.0),
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().plusDays(3).toString(), 12.0),
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().plusDays(4).toString(), 0.0),
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().plusDays(5).toString(), 6.0),
            com.jalsanchay.tracker.model.ForecastDay(LocalDate.now().plusDays(6).toString(), 4.0)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedIfEmpty(seedEntries)
        }
    }

    fun completeSetup(settings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings.copy(setupDone = true))
            messages.emit("Changes saved successfully")
        }
    }

    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            val recalculated = recalculateEntries(
                uiState.value.entries,
                settings.roofArea,
                settings.unit,
                settings.runoffCoeff,
                settings.tankCapacity
            )
            repository.saveSettings(settings)
            repository.replaceEntries(recalculated)
            if (settings.rainfallReminderEnabled) reminderScheduler.scheduleDaily() else reminderScheduler.cancel()
            messages.emit("Settings updated. Data recalculated.")
        }
    }

    fun saveRainfallEntry(id: Long, date: String, rainfallMm: Double) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            val litres = calculateWaterCollected(settings.roofArea, settings.unit, rainfallMm, settings.runoffCoeff, settings.tankCapacity)
            repository.saveEntry(
                RainfallEntry(
                    id = id,
                    date = date,
                    rainfallMm = rainfallMm,
                    litresCollected = litres,
                    createdAt = System.currentTimeMillis()
                )
            )
            messages.emit("Entry saved")
        }
    }

    fun deleteEntry(entry: RainfallEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            messages.emit("Entry deleted")
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            reminderScheduler.cancel()
            repository.reset()
            messages.emit("App reset")
        }
    }

    fun exportPdf(onExported: (File) -> Unit) {
        viewModelScope.launch {
            val file = pdfExporter.exportMonthlyReport(uiState.value.monthlyReports)
            onExported(file)
            messages.emit("PDF exported")
        }
    }

    fun getAiTip() {
        viewModelScope.launch {
            messages.emit(aiTipService.offlinePlaceholder(uiState.value.settings))
        }
    }

    fun dismissMilestone() {
        currentMilestone.value?.let { milestone ->
            dismissedMilestones.update { it + milestone }
        }
    }

    fun setLocationName(value: String) {
        locationName.value = value
    }

    fun fetchSeasonAnalysis() {
        viewModelScope.launch {
            aiSeasonInsight.value = UiState.Loading
            val state = uiState.value
            val total = state.entries.sumOf { it.litresCollected }
            val response = aiTipService.getSeasonAnalysis(state.settings, state.monthlyReports, total, total / 135.0)
            aiSeasonInsight.value = if (response == "Unable to generate response.") UiState.Error(response) else UiState.Success(response)
        }
    }

    fun fetchTips() {
        viewModelScope.launch {
            aiTips.value = UiState.Loading
            val state = uiState.value
            val total = state.entries.sumOf { it.litresCollected }
            val response = aiTipService.getTips(
                setup = state.settings,
                entryCount = state.entries.size,
                allSaved = total,
                bestDay = bestDayLitres.value,
                streak = streakDays.value,
                locName = locationName.value.ifBlank { "Not set" },
                season = seasonLabel(LocalDate.now().monthValue)
            )
            aiTips.value = if (response == "Unable to generate response.") UiState.Error(response) else UiState.Success(response)
        }
    }

    fun askGlossary(question: String) {
        viewModelScope.launch {
            val answer = aiTipService.askGlossary(question, uiState.value.settings)
            glossaryAnswer.emit(question to answer)
        }
    }

    private fun milestoneMessage(oldTotal: Double, newTotal: Double): String? {
        if (oldTotal == 0.0) return null
        return listOf(500.0, 1000.0, 5000.0, 10000.0)
            .firstOrNull { oldTotal < it && newTotal >= it }
            ?.let { "Milestone reached: ${it.toInt()} L saved" }
    }

    fun today(): String = LocalDate.now().toString()

    private fun countStreak(dates: Set<String>): Int {
        var cursor = LocalDate.now()
        var streak = 0
        while (dates.contains(cursor.toString())) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun seasonLabel(month: Int): String {
        return when (month) {
            in 3..5 -> "Pre-Monsoon"
            in 6..9 -> "SW Monsoon"
            in 10..12 -> "NE Monsoon"
            else -> "Dry Season"
        }
    }

    private val seedEntries = listOf(
        RainfallEntry(date = "2025-05-10", rainfallMm = 8.0, litresCollected = 504.7),
        RainfallEntry(date = "2025-05-18", rainfallMm = 14.0, litresCollected = 882.5),
        RainfallEntry(date = "2025-06-05", rainfallMm = 28.0, litresCollected = 1765.0),
        RainfallEntry(date = "2025-06-12", rainfallMm = 35.0, litresCollected = 2206.3),
        RainfallEntry(date = "2025-06-20", rainfallMm = 22.0, litresCollected = 1386.9),
        RainfallEntry(date = "2025-06-28", rainfallMm = 41.0, litresCollected = 2584.7),
        RainfallEntry(date = "2025-07-03", rainfallMm = 52.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-07-09", rainfallMm = 38.0, litresCollected = 2395.7),
        RainfallEntry(date = "2025-07-15", rainfallMm = 60.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-07-22", rainfallMm = 45.0, litresCollected = 2836.8),
        RainfallEntry(date = "2025-08-02", rainfallMm = 48.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-08-11", rainfallMm = 55.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-08-19", rainfallMm = 32.0, litresCollected = 2017.2),
        RainfallEntry(date = "2025-08-27", rainfallMm = 40.0, litresCollected = 2521.6),
        RainfallEntry(date = "2025-09-04", rainfallMm = 30.0, litresCollected = 1891.2),
        RainfallEntry(date = "2025-09-15", rainfallMm = 18.0, litresCollected = 1134.7),
        RainfallEntry(date = "2025-09-24", rainfallMm = 22.0, litresCollected = 1386.9),
        RainfallEntry(date = "2025-10-08", rainfallMm = 25.0, litresCollected = 1576.0),
        RainfallEntry(date = "2025-10-20", rainfallMm = 15.0, litresCollected = 945.6),
        RainfallEntry(date = "2025-11-05", rainfallMm = 20.0, litresCollected = 1260.8),
        RainfallEntry(date = "2025-11-18", rainfallMm = 12.0, litresCollected = 756.5),
        RainfallEntry(date = "2025-12-10", rainfallMm = 6.0, litresCollected = 378.2),
        RainfallEntry(date = "2026-01-14", rainfallMm = 4.0, litresCollected = 252.2),
        RainfallEntry(date = "2026-02-20", rainfallMm = 10.0, litresCollected = 630.4),
        RainfallEntry(date = "2026-03-08", rainfallMm = 16.0, litresCollected = 1008.6),
        RainfallEntry(date = "2026-04-12", rainfallMm = 18.0, litresCollected = 1134.7),
        RainfallEntry(date = "2026-04-28", rainfallMm = 12.0, litresCollected = 756.5),
        RainfallEntry(date = "2026-04-29", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-04-30", rainfallMm = 25.0, litresCollected = 1576.0)
    )
}

class TrackerViewModelFactory(
    private val repository: TrackerRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TrackerViewModel(repository, context.applicationContext) as T
    }
}
