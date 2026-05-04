package com.jalsanchay.tracker.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.model.UiState
import com.jalsanchay.tracker.util.calculateImpactDays
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import java.time.LocalDate

@Composable
internal fun ReportsScreen(uiState: TrackerUiState, palette: AppPalette, viewModel: TrackerViewModel, onOpenCalculator: () -> Unit) {
    val context = LocalContext.current
    val bestMonth by viewModel.bestMonth.collectAsStateWithLifecycle()
    val previousMonthLitres by viewModel.previousMonthLitres.collectAsStateWithLifecycle()
    val aiSeasonInsight by viewModel.aiSeasonInsight.collectAsStateWithLifecycle()
    val pdfUri by viewModel.exportedPdfUri.collectAsStateWithLifecycle()
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val allYears = uiState.monthlyReports.map { it.monthKey.take(4).toInt() }.distinct().sorted()
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    val reports = uiState.monthlyReports.filter { it.monthKey.take(4).toInt() == selectedYear }
    val totalMm = uiState.entries.sumOf { it.rainfallMm }
    val totalLitres = uiState.entries.sumOf { it.litresCollected }
    val monthsTracked = uiState.monthlyReports.size
    val yearRain = reports.sumOf { it.totalRainfallMm }
    val yearLitres = reports.sumOf { it.totalWaterSaved }
    val latest = reports.sortedBy { it.monthKey }.lastOrNull()
    val previous = reports.sortedBy { it.monthKey }.dropLast(1).lastOrNull()
    val diffDays = if (latest != null && previous != null) calculateImpactDays(latest.totalWaterSaved) - calculateImpactDays(previousMonthLitres) else 0.0

    LaunchedEffect(pdfUri) {
        pdfUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareLauncher.launch(Intent.createChooser(shareIntent, "Share Report"))
            viewModel.clearExportedUri()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(palette.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Monthly Reports", color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenCalculator) {
                Icon(Icons.Default.BarChart, contentDescription = "Open calculator", tint = palette.primary)
            }
        }
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { selectedYear = allYears.filter { it < selectedYear }.maxOrNull() ?: selectedYear }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous year")
                }
                Text("$selectedYear", color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { selectedYear = allYears.filter { it > selectedYear }.minOrNull() ?: selectedYear }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next year")
                }
            }
            AppCard(palette) {
                Text("All-time summary", color = palette.text, fontWeight = FontWeight.Bold)
                Text("${"%.1f".format(totalMm)}mm · ${totalLitres.toInt()}L · ${"%.1f".format(calculateImpactDays(totalLitres))} days · $monthsTracked months", color = palette.muted)
            }
            if (reports.isEmpty()) {
                AppCard(palette) { Text("Not enough data to display trends", color = palette.text) }
            }
            reports.forEach {
                AppCard(palette) {
                    Text(monthLabel(it.monthKey), color = palette.text, fontWeight = FontWeight.Bold)
                    Text("${"%.1f".format(it.totalRainfallMm)} mm rainfall", color = palette.muted)
                    Text("${"%.1f".format(it.totalWaterSaved)} L saved | ${"%.1f".format(it.impactDays)} days", color = palette.muted)
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)).background(palette.surfaceStrong)) {
                        val max = reports.maxOfOrNull { report -> report.totalWaterSaved }?.coerceAtLeast(1.0) ?: 1.0
                        Box(Modifier.fillMaxWidth((it.totalWaterSaved / max).toFloat()).height(10.dp).background(palette.primary))
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text("${"%.1f".format(yearRain)}mm", fontWeight = FontWeight.Bold)
                    Text("${yearLitres.toInt()}L", fontWeight = FontWeight.Bold)
                }
            }
            bestMonth?.let {
                AppCard(palette) {
                    Text("Best month: ${monthLabel(it.first)} — ${it.second.toInt()}L · ${"%.1f".format(calculateImpactDays(it.second))} days", color = palette.text, fontWeight = FontWeight.Bold)
                }
            }
            if (latest != null && previous != null) {
                AppCard(palette) {
                    Row {
                        Text("${monthLabel(latest.monthKey)} saved ${"%.1f".format(calculateImpactDays(latest.totalWaterSaved))} days — ", color = palette.muted)
                        Text("${if (diffDays >= 0) "↑" else "↓"} ${"%.1f".format(kotlin.math.abs(diffDays))} days", color = if (diffDays >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                    Text("vs ${monthLabel(previous.monthKey)}", color = palette.muted)
                }
            }
            AppCard(palette) {
                Text("AI Season Analysis", color = palette.text, fontWeight = FontWeight.Bold)
                Button(onClick = viewModel::fetchSeasonAnalysis, enabled = uiState.isOnline) { Text("Get AI Insight") }
                when (val state = aiSeasonInsight) {
                    UiState.Idle -> Unit
                    UiState.Loading -> CircularProgressIndicator()
                    is UiState.Success -> Text(state.value, color = palette.muted)
                    is UiState.Error -> Text("Unable to generate response. Tap to retry.", color = palette.danger)
                }
            }
        }
        Button({ viewModel.exportPdfReport(context) }, Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Export PDF Report")
        }
    }
}
