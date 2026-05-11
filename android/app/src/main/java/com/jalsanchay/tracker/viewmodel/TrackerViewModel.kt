package com.jalsanchay.tracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jalsanchay.tracker.ai.AiTipService
import com.jalsanchay.tracker.data.JalSanchayDatabase
import com.jalsanchay.tracker.data.RainfallEntry
import com.jalsanchay.tracker.data.TrackerRepository
import com.jalsanchay.tracker.data.UserSetup
import com.jalsanchay.tracker.model.AsyncState
import com.jalsanchay.tracker.model.AnalyticsData
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.model.UiState
import com.jalsanchay.tracker.notifications.RainPredictionWorker
import com.jalsanchay.tracker.util.Calculations
import com.jalsanchay.tracker.util.ImportResult
import com.jalsanchay.tracker.util.LocationHelper
import com.jalsanchay.tracker.util.NetworkMonitor
import com.jalsanchay.tracker.util.PdfExporter
import com.jalsanchay.tracker.util.WeatherResult
import com.jalsanchay.tracker.util.exportToJson
import com.jalsanchay.tracker.util.fetchWeatherForLocation
import com.jalsanchay.tracker.util.importFromJson
import com.jalsanchay.tracker.widget.JalSanchayWidget
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JalSanchayDatabase.getInstance(application)
    private val repo = TrackerRepository(db)
    private val networkMonitor = NetworkMonitor(application)
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val pdfExporter = PdfExporter(application)
    private val aiTipService = AiTipService()
    val messages = MutableSharedFlow<String>()

    private val _setup = repo.setup
    private val _entries = repo.allEntries
    private val _totalLitres = repo.totalLitres

    val locationName = MutableStateFlow("")
    val forecastDays = MutableStateFlow(emptyList<com.jalsanchay.tracker.model.ForecastDay>())
    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()
    val aiSeasonInsight = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiTips = MutableStateFlow<UiState<String>>(UiState.Idle)
    val glossaryAnswer = MutableSharedFlow<Pair<String, String>>()

    init {
        viewModelScope.launch {
            _setup.collect { saved ->
                val loc = saved?.locationName.orEmpty()
                if (loc.isNotBlank() && locationName.value.isBlank()) {
                    locationName.value = loc
                    fetchWeather()
                }
            }
        }
    }

    val setup: StateFlow<UserSetup?> = _setup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allEntries: StateFlow<List<RainfallEntry>> = _entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLitres: StateFlow<Double> = _totalLitres
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val entryCount: StateFlow<Int> = repo.entryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Consolidated stats from entries (single map instead of 8 separate ones) ──
    private data class DerivedStats(
        val todaySaved: Double = 0.0,
        val streak: Int = 0,
        val bestDay: Double = 0.0,
        val avgMonthly: Double = 0.0,
        val dryDays: Int = 0,
        val monthlyTotals: List<Calculations.MonthlyTotal> = emptyList(),
        val distinctMonths: Int = 0,
        val currentMonthProgress: Float = 0f,
        val lastLoggedDate: String? = null
    )

    private val derivedStats: StateFlow<DerivedStats> = _entries.map { entries ->
        val today = java.time.LocalDate.now().toString()
        val currentMonthKey = today.substring(0, 7)
        DerivedStats(
            todaySaved = entries.filter { it.date == today }.sumOf { it.litresCollected },
            streak = Calculations.calculateStreak(entries),
            bestDay = Calculations.getBestDayLitres(entries),
            avgMonthly = Calculations.getAvgMonthlyLitres(entries),
            dryDays = Calculations.getDryDaysCount(entries),
            monthlyTotals = Calculations.getMonthlyTotals(entries),
            distinctMonths = entries.map { it.date.take(7) }.distinct().size,
            currentMonthProgress = Calculations.getCurrentMonthProgress(entries, currentMonthKey),
            lastLoggedDate = entries.maxByOrNull { it.date }?.date
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DerivedStats())

    val todaySaved: StateFlow<Double> = derivedStats.map { it.todaySaved }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val impactScore: StateFlow<Double> = _totalLitres.map {
        Calculations.calculateImpactScore(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val streakDays: StateFlow<Int> = derivedStats.map { it.streak }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bestDayLitres: StateFlow<Double> = derivedStats.map { it.bestDay }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val avgMonthlyLitres: StateFlow<Double> = derivedStats.map { it.avgMonthly }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dryDaysCount: StateFlow<Int> = derivedStats.map { it.dryDays }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyTotals: StateFlow<List<Calculations.MonthlyTotal>> = derivedStats.map { it.monthlyTotals }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctMonthCount: StateFlow<Int> = derivedStats.map { it.distinctMonths }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bestMonth: StateFlow<Pair<String, Double>?> =
        monthlyTotals.map { totals ->
            totals.maxByOrNull { it.totalLitres }?.let { it.monthKey to it.totalLitres }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val previousMonthLitres: StateFlow<Double> = monthlyTotals.map { totals ->
        totals.sortedBy { it.monthKey }.dropLast(1).lastOrNull()?.totalLitres ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val analyticsData: StateFlow<AnalyticsData> = combine(
        _entries,
        derivedStats
    ) { entries, stats ->
        AnalyticsData(
            monthlyTotals = stats.monthlyTotals,
            bestDay = entries.maxByOrNull { it.litresCollected },
            streakDays = stats.streak,
            dryDaysCount = stats.dryDays,
            avgMonthly = stats.avgMonthly,
            seasonBreakdown = buildSeasonBreakdown(stats.monthlyTotals)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsData())

    val currentMonthProgress: StateFlow<Float> = derivedStats.map { it.currentMonthProgress }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val lastLoggedDate: StateFlow<String?> = derivedStats.map { it.lastLoggedDate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val litresToFill: StateFlow<Double> = combine(todaySaved, _setup) { today, setup ->
        val cap = setup?.tankCapacity ?: 3000.0
        maxOf(cap - today, 0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000.0)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _weatherCacheAge = MutableStateFlow<Long?>(null)
    val weatherCacheAge: StateFlow<Long?> = _weatherCacheAge.asStateFlow()

    private val _weatherForecastJson = MutableStateFlow<String?>(null)
    val weatherForecastJson: StateFlow<String?> = _weatherForecastJson.asStateFlow()

    private val _exportedPdfUri = MutableStateFlow<Uri?>(null)
    val exportedPdfUri: StateFlow<Uri?> = _exportedPdfUri.asStateFlow()

    private val dismissedMilestones = MutableStateFlow<Set<Double>>(emptySet())

    val currentMilestone: StateFlow<Double?> = combine(_totalLitres, dismissedMilestones) { total, dismissed ->
        val thresholds = listOf(500.0, 1000.0, 5000.0, 10000.0)
        thresholds
            .filter { total >= it && it !in dismissed }
            .maxOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<TrackerUiState> = combine(
        _setup,
        _entries,
        todaySaved,
        _totalLitres,
        impactScore,
        streakDays,
        bestDayLitres,
        avgMonthlyLitres,
        dryDaysCount,
        currentMonthProgress,
        lastLoggedDate,
        litresToFill,
        monthlyTotals,
        bestMonth,
        currentMilestone,
        isOnline
    ) { values ->
        val setup = values[0] as? UserSetup ?: UserSetup()
        val entries = values[1] as List<RainfallEntry>
        val today = values[2] as Double
        val total = values[3] as Double
        val impact = values[4] as Double
        val monthly = values[12] as List<Calculations.MonthlyTotal>
        TrackerUiState(
            settings = setup,
            setupComplete = setup.setupDone,
            entries = entries,
            todaySaved = today,
            monthSaved = entries.filter { it.date.startsWith(java.time.LocalDate.now().toString().substring(0, 7)) }.sumOf { it.litresCollected },
            allTimeSaved = total,
            impactScore = impact,
            tankPercentage = Calculations.calculateTankPercentage(today, setup.tankCapacity),
            streakDays = values[5] as Int,
            bestDayLitres = values[6] as Double,
            avgMonthlyLitres = values[7] as Double,
            dryDaysCount = values[8] as Int,
            currentMonthProgress = values[9] as Float,
            lastLoggedDate = values[10] as String?,
            litresToFill = values[11] as Double,
            monthlyTotals = monthly,
            monthlyReports = monthly,
            bestMonth = values[13] as Pair<String, Double>?,
            currentMilestone = values[14] as Double?,
            isOnline = values[15] as Boolean,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerUiState())

    fun dismissMilestone(threshold: Double) {
        dismissedMilestones.value = dismissedMilestones.value + threshold
    }

    fun dismissMilestone() {
        // Dismiss ALL thresholds unconditionally — prevents re-showing lower milestones
        val allThresholds = setOf(500.0, 1000.0, 5000.0, 10000.0, 25000.0, 50000.0, 100000.0)
        dismissedMilestones.value = allThresholds
    }

    fun saveEntry(date: String, rainfallMm: Double) {
        viewModelScope.launch {
            val s = setup.value ?: return@launch
            val litres = Calculations.calculateWaterCollected(
                roofArea = s.roofArea,
                unit = s.unit,
                rainfallMm = rainfallMm,
                runoffCoeff = s.runoffCoefficient,
                tankCapacity = s.tankCapacity
            )
            repo.insertEntry(
                RainfallEntry(
                    date = date,
                    rainfallMm = rainfallMm,
                    litresCollected = litres
                )
            )
            refreshWidgets()
        }
    }

    fun updateEntry(entry: RainfallEntry, newMm: Double, newDate: String) {
        viewModelScope.launch {
            val s = setup.value ?: return@launch
            val newLitres = Calculations.calculateWaterCollected(
                roofArea = s.roofArea,
                unit = s.unit,
                rainfallMm = newMm,
                runoffCoeff = s.runoffCoefficient,
                tankCapacity = s.tankCapacity
            )
            repo.updateEntry(
                entry.copy(
                    date = newDate,
                    rainfallMm = newMm,
                    litresCollected = newLitres
                )
            )
            refreshWidgets()
        }
    }

    fun deleteEntry(entry: RainfallEntry) {
        viewModelScope.launch {
            repo.deleteEntry(entry)
            refreshWidgets()
        }
    }

    fun saveSetup(setup: UserSetup) {
        viewModelScope.launch {
            val normalized = setup.copy(runoffCoefficient = setup.runoffCoeff)
            repo.saveSetup(normalized)
            repo.recalculateAllEntries(normalized)
        }
    }

    fun saveSettings(setup: UserSetup) = saveSetup(setup)

    fun completeSetup(setup: UserSetup) = saveSetup(setup.copy(setupDone = true))

    fun saveRainfallEntry(id: Long, date: String, rainfallMm: Double) {
        if (id == 0L) saveEntry(date, rainfallMm) else {
            val current = allEntries.value.firstOrNull { it.id.toLong() == id } ?: return
            updateEntry(current, rainfallMm, date)
        }
    }

    fun exportPdf(onExported: (File) -> Unit) {
        viewModelScope.launch {
            onExported(pdfExporter.exportMonthlyReport(monthlyTotals.value))
        }
    }

    fun exportPdfReport(context: Context) {
        viewModelScope.launch {
            try {
                val s = _setup.filterNotNull().first()
                val entries = repo.getAllEntriesList()
                if (entries.isEmpty()) {
                    showSnackbar("No data to export")
                    return@launch
                }
                val total = entries.sumOf { it.litresCollected }
                val uri = PdfExporter.generateReport(
                    context = context,
                    setup = s,
                    monthly = Calculations.getMonthlyTotals(entries),
                    totalLitres = total,
                    impactScore = Calculations.calculateImpactScore(total)
                )
                _exportedPdfUri.value = uri
                showSnackbar("PDF report generated")
            } catch (e: Exception) {
                showSnackbar("PDF export failed: ${e.message}")
            }
        }
    }

    fun clearExportedUri() {
        _exportedPdfUri.value = null
    }

    fun today(): String = java.time.LocalDate.now().toString()

    fun setLocationName(value: String) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            showSnackbar("Please enter a city name")
            return
        }
        locationName.value = trimmed
        viewModelScope.launch {
            val current = _setup.filterNotNull().first()
            repo.saveSetup(current.copy(locationName = trimmed))
            // Invalidate weather cache so we force a fresh fetch for the new location
            try { repo.saveWeatherCache("", "") } catch (_: Exception) {}
            showSnackbar("Location set to $trimmed")
            fetchWeather()
        }
    }

    fun detectLocation(context: Context) {
        viewModelScope.launch {
            val helper = LocationHelper(context, httpClient)
            if (!helper.hasLocationPermission()) {
                showSnackbar("Location permission required")
                return@launch
            }
            val city = helper.getCurrentCityName()
            if (city != null) {
                val current = _setup.filterNotNull().first()
                locationName.value = city
                repo.saveSetup(current.copy(locationName = city))
                try { repo.saveWeatherCache("", "") } catch (_: Exception) {}
                fetchWeather()
                showSnackbar("Location set to $city")
            } else {
                showSnackbar("Could not detect location")
            }
        }
    }

    fun fetchSeasonAnalysis() {
        viewModelScope.launch {
            if (!isOnline.value) {
                showSnackbar("No connection")
                return@launch
            }
            aiSeasonInsight.value = UiState.Loading
            val settings = setup.value ?: UserSetup()
            val response = aiTipService.getSeasonAnalysis(
                setup = settings,
                monthlyData = monthlyTotals.value,
                allSaved = totalLitres.value,
                impact = impactScore.value
            )
            aiSeasonInsight.value = UiState.Success(response)
        }
    }

    fun fetchTips() {
        viewModelScope.launch {
            if (!isOnline.value) {
                showSnackbar("No connection")
                return@launch
            }
            aiTips.value = UiState.Loading
            val response = aiTipService.getTips(
                setup = setup.value ?: UserSetup(),
                entryCount = allEntries.value.size,
                allSaved = totalLitres.value,
                bestDay = bestDayLitres.value,
                streak = streakDays.value,
                locName = locationName.value.ifBlank { setup.value?.locationName.orEmpty() },
                season = currentSeasonLabel()
            )
            aiTips.value = UiState.Success(response)
        }
    }

    fun askGlossary(question: String) {
        viewModelScope.launch {
            if (!isOnline.value) {
                showSnackbar("No connection")
                return@launch
            }
            glossaryAnswer.emit(question to aiTipService.askGlossary(question, setup.value ?: UserSetup()))
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            repo.replaceEntries(emptyList())
            repo.saveSetup(UserSetup())
        }
    }

    fun seedDatabase() {
        viewModelScope.launch {
            repo.seedIfEmpty()
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            val loc = locationName.value.ifBlank { setup.value?.locationName.orEmpty() }
            if (loc.isBlank()) return@launch
            // Check cache first — only use if same location and not expired
            val cached = repo.getWeatherCache()
            if (cached != null && cached.locationName.equals(loc, ignoreCase = true)
                && cached.forecastJson.isNotBlank()) {
                _weatherCacheAge.value = cached.fetchedAt
                _weatherForecastJson.value = cached.forecastJson
                forecastDays.value = parseForecastDays(cached.forecastJson)
                return@launch
            }
            // Fetch fresh weather data from Open-Meteo
            _weatherLoading.value = true
            when (val result = fetchWeatherForLocation(loc, httpClient)) {
                is WeatherResult.Success -> {
                    repo.saveWeatherCache(result.resolvedName, result.forecastJson)
                    scheduleRainPredictionCheck()
                    _weatherCacheAge.value = System.currentTimeMillis()
                    _weatherForecastJson.value = result.forecastJson
                    locationName.value = result.resolvedName
                    forecastDays.value = result.forecast.days.map {
                        com.jalsanchay.tracker.model.ForecastDay(it.date, it.precipitationSum)
                    }
                }
                is WeatherResult.Error -> showSnackbar("Weather: ${result.message}")
            }
            _weatherLoading.value = false
        }
    }

    private fun parseForecastDays(json: String): List<com.jalsanchay.tracker.model.ForecastDay> {
        return try {
            val daily = JSONObject(json).getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val rain = daily.getJSONArray("precipitation_sum")
            (0 until dates.length()).map {
                com.jalsanchay.tracker.model.ForecastDay(dates.getString(it), rain.optDouble(it, 0.0))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val _exportedDataUri = MutableStateFlow<Uri?>(null)
    val exportedDataUri: StateFlow<Uri?> = _exportedDataUri.asStateFlow()

    fun clearExportedDataUri() { _exportedDataUri.value = null }

    fun exportData(context: Context) {
        viewModelScope.launch {
            try {
                val settings = _setup.filterNotNull().first()
                val entries = repo.getAllEntriesList()
                if (entries.isEmpty()) {
                    showSnackbar("No entries to export")
                    return@launch
                }
                val uri = exportToJson(context, settings, entries)
                _exportedDataUri.value = uri
                showSnackbar("Backup exported (${entries.size} entries)")
            } catch (e: Exception) {
                showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            when (val result = importFromJson(context, uri)) {
                is ImportResult.Success -> {
                    val settingsWithSetup = result.settings.copy(setupDone = true)
                    repo.saveSetup(settingsWithSetup)
                    repo.replaceEntries(result.entries)
                    repo.recalculateAllEntries(settingsWithSetup)
                    refreshWidgets()
                    showSnackbar("Imported ${result.entries.size} entries")
                }
                is ImportResult.Error -> showSnackbar("Import failed: ${result.message}")
            }
        }
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private suspend fun refreshWidgets() {
        val context = getApplication<Application>()
        GlanceAppWidgetManager(context)
            .getGlanceIds(JalSanchayWidget::class.java)
            .forEach { JalSanchayWidget().update(context, it) }
    }

    private fun currentSeasonLabel(): String {
        return when (java.time.LocalDate.now().monthValue) {
            in 3..5 -> "Pre-Monsoon"
            in 6..9 -> "SW Monsoon"
            in 10..12 -> "NE Monsoon"
            else -> "Dry Season"
        }
    }

    private fun scheduleRainPredictionCheck() {
        val request = OneTimeWorkRequestBuilder<RainPredictionWorker>()
            .setInitialDelay(2, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    private fun buildSeasonBreakdown(monthly: List<Calculations.MonthlyTotal>): Map<String, Double> {
        return monthly.groupBy {
            when (it.monthKey.takeLast(2).toInt()) {
                in 6..9 -> "SW Monsoon"
                in 10..12 -> "NE Monsoon"
                in 3..5 -> "Pre-Monsoon"
                else -> "Dry"
            }
        }.mapValues { (_, totals) -> totals.sumOf { it.totalLitres } }
            .let { totals ->
                mapOf(
                    "SW Monsoon" to (totals["SW Monsoon"] ?: 0.0),
                    "NE Monsoon" to (totals["NE Monsoon"] ?: 0.0),
                    "Pre-Monsoon" to (totals["Pre-Monsoon"] ?: 0.0),
                    "Dry" to (totals["Dry"] ?: 0.0)
                )
            }
    }
}
