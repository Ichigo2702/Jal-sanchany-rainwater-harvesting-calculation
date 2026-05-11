package com.jalsanchay.tracker.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jalsanchay.tracker.model.ForecastDay
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.util.calculateImpactDays
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun DashboardScreen(
    uiState: TrackerUiState,
    palette: AppPalette,
    viewModel: TrackerViewModel,
    onLog: () -> Unit,
    onCalculator: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val streak by viewModel.streakDays.collectAsStateWithLifecycle()
    val bestDay by viewModel.bestDayLitres.collectAsStateWithLifecycle()
    val averageMonth by viewModel.avgMonthlyLitres.collectAsStateWithLifecycle()
    val dryDays by viewModel.dryDaysCount.collectAsStateWithLifecycle()
    val monthProgress by viewModel.currentMonthProgress.collectAsStateWithLifecycle()
    val distinctMonths by viewModel.distinctMonthCount.collectAsStateWithLifecycle()
    val milestone by viewModel.currentMilestone.collectAsStateWithLifecycle()
    val locationName by viewModel.locationName.collectAsStateWithLifecycle()
    val forecastDays by viewModel.forecastDays.collectAsStateWithLifecycle()
    val weatherCacheAge by viewModel.weatherCacheAge.collectAsStateWithLifecycle()
    val weatherLoading by viewModel.weatherLoading.collectAsStateWithLifecycle()
    var forecastExpanded by remember { mutableStateOf(false) }
    var locationDraft by remember(locationName) { mutableStateOf(locationName) }
    var editingLocation by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.detectLocation(context) else onPermissionDenied()
    }
    val entries = uiState.entries
    val today = LocalDate.now().toString()
    val todayLitres = entries.filter { it.date == today }.sumOf { it.litresCollected }
    val total = entries.sumOf { it.litresCollected }
    val month = entries.filter { it.date.take(7) == today.take(7) }.sumOf { it.litresCollected }
    val tankPercent = if (uiState.settings.tankCapacity > 0) (todayLitres / uiState.settings.tankCapacity).coerceAtMost(1.0) else 0.0
    val season = seasonInfo(LocalDate.now().monthValue)
    val lastLogged = entries.maxByOrNull { it.date }?.date ?: "None"
    val litresToFill = (uiState.settings.tankCapacity - todayLitres).coerceAtLeast(0.0)
    val nextRain = forecastDays.firstOrNull { it.date > today && it.precipitationSum > 5.0 }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(palette.surface).padding(16.dp)) {
            Text(greeting(), color = palette.muted, style = MaterialTheme.typography.labelMedium)
            Text("Jal-Sanchay Tracker", color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(LocalDate.now().toString(), color = palette.muted, style = MaterialTheme.typography.bodySmall)
            Text(
                season.label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(999.dp)).background(season.color).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            milestone?.let {
                // Auto-dismiss milestone after 5 seconds
                androidx.compose.runtime.LaunchedEffect(milestone) {
                    kotlinx.coroutines.delay(5000)
                    viewModel.dismissMilestone()
                }
                AppCard(palette) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("🎉 Milestone reached!", color = palette.text, fontWeight = FontWeight.Bold)
                            Text("${total.toInt()}L saved — ${"%.1f".format(calculateImpactDays(total))} days of supply", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = viewModel::dismissMilestone) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss milestone", tint = palette.muted)
                        }
                    }
                }
            }
            if (todayLitres == 0.0) {
                AppCard(palette) {
                    Text("Today's harvest", color = palette.text, style = MaterialTheme.typography.titleMedium)
                    Text("Did it rain today?", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onLog, Modifier.weight(1f)) { Text("Log Rainfall") }
                        OutlinedButton(
                            onClick = { viewModel.saveEntry(viewModel.today(), 0.0) },
                            modifier = Modifier.weight(1f)
                        ) { Text("No Rain Today") }
                    }
                }
            } else {
                AppCard(palette, Modifier.semantics { contentDescription = "Impact score ${"%.1f".format(calculateImpactDays(total))} days" }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WaterTank(tankPercent, palette)
                            Text("${litresToFill.toInt()} L to fill · Last logged: $lastLogged", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text("${"%.1f".format(calculateImpactDays(total))} days", color = palette.text, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                            Text("days of water supply saved", color = palette.muted, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(16.dp))
                            Text("${total.toInt()} L", color = palette.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("collected all-time", color = palette.muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (entries.isEmpty()) AppCard(palette) { Text("Start tracking to see your savings", color = palette.text, fontWeight = FontWeight.Bold) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("This Month", "${month.toInt()} L", palette, Modifier.weight(1f))
                AppCard(palette, Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (streak > 0) Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF7043))
                        Text(if (streak > 0) "$streak day streak" else "Start your streak", color = palette.text, fontWeight = FontWeight.Bold)
                    }
                }
            }
            AppCard(palette) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatColumn("Best Day", "${bestDay.toInt()} L", palette, Modifier.weight(1f))
                    HorizontalDivider(Modifier.height(36.dp).width(1.dp), color = palette.surfaceStrong)
                    StatColumn("Avg/Month", "${averageMonth.toInt()} L", palette, Modifier.weight(1f))
                    HorizontalDivider(Modifier.height(36.dp).width(1.dp), color = palette.surfaceStrong)
                    StatColumn("Dry Days", "$dryDays", palette, Modifier.weight(1f))
                }
            }
            if (distinctMonths >= 2) {
                AppCard(palette) {
                    Text("${YearMonth.now().month.name.lowercase().replaceFirstChar { it.uppercase() }} — ${(monthProgress * 100).toInt()}% of monthly average", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { monthProgress }, modifier = Modifier.fillMaxWidth())
                }
            }
            nextRain?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Umbrella, contentDescription = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Rain expected ${LocalDate.parse(it.date).dayOfWeek.name.lowercase().replaceFirstChar { c -> c.uppercase() }} · ${it.precipitationSum.toInt()}mm", fontWeight = FontWeight.Bold)
                            Text("Prepare your tank", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ── Location & Forecast Section ──
            if (locationName.isBlank()) {
                TextButton(onClick = { forecastExpanded = !forecastExpanded }) {
                    Text("Set your location for rain forecast →")
                }
                AnimatedVisibility(forecastExpanded) {
                    LocationInputCard(palette, locationDraft, { locationDraft = it },
                        onSave = { viewModel.setLocationName(locationDraft) },
                        onDetect = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                    )
                }
            } else {
                AppCard(palette) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("7-day forecast for $locationName", color = palette.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingLocation = !editingLocation }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit location", tint = palette.muted, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.fetchWeather() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh weather", tint = palette.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (weatherLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Fetching weather...", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        }
                    }
                    weatherCacheAge?.let {
                        val hoursAgo = ((System.currentTimeMillis() - it) / (60 * 60 * 1000)).coerceAtLeast(0)
                        Text("Updated ${hoursAgo}h ago", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    }
                    // Visual forecast day cards
                    if (forecastDays.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            forecastDays.forEach { day ->
                                ForecastDayCard(day, palette)
                            }
                        }
                    } else if (!weatherLoading) {
                        Text("No forecast data available. Tap refresh to fetch.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
                AnimatedVisibility(editingLocation) {
                    LocationInputCard(palette, locationDraft, { locationDraft = it },
                        onSave = { viewModel.setLocationName(locationDraft); editingLocation = false },
                        onDetect = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                    )
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onCalculator, Modifier.fillMaxWidth()) { Text("Calculator") }
            Button(onLog, Modifier.fillMaxWidth().semantics { contentDescription = "Log rainfall" }) { Text("Log Rainfall") }
        }
    }
}

@Composable
private fun ForecastDayCard(day: ForecastDay, palette: AppPalette) {
    val date = LocalDate.parse(day.date)
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val hasRain = day.precipitationSum > 0.5
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surfaceStrong),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(72.dp)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(dayName, style = MaterialTheme.typography.labelSmall, color = palette.muted)
            Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.text)
            Icon(
                if (hasRain) Icons.Default.WaterDrop else Icons.Default.WbSunny,
                contentDescription = null,
                tint = if (hasRain) Color(0xFF42A5F5) else Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
            Text(
                "${day.precipitationSum.toInt()}mm",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (hasRain) palette.primary else palette.muted
            )
        }
    }
}

@Composable
private fun LocationInputCard(
    palette: AppPalette,
    locationDraft: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onDetect: () -> Unit
) {
    AppCard(palette) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(locationDraft, onDraftChange, label = { Text("Location") }, modifier = Modifier.weight(1f))
            IconButton(onClick = onDetect) {
                Icon(Icons.Default.LocationOn, contentDescription = "Detect location")
            }
        }
        Button(onSave, enabled = locationDraft.isNotBlank()) { Text("Save Location") }
    }
}
