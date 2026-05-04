package com.jalsanchay.tracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jalsanchay.tracker.model.AnalyticsData
import com.jalsanchay.tracker.model.TrackerUiState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.jalsanchay.tracker.util.calculateImpactDays

@Composable
fun AnalyticsScreen(uiState: TrackerUiState, analyticsData: AnalyticsData) {
    // Animated counters
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }
    val animFraction = animProgress.value

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AnimatedSummaryStrip(uiState, analyticsData, animFraction)
        ImpactHighlights(uiState, analyticsData, animFraction)
        MonthlyCollectionChart(analyticsData)
        RainfallTrendChart(analyticsData)
        AnimatedSeasonContribution(analyticsData, animFraction)
        PersonalRecords(uiState, analyticsData)
    }
}

@Composable
private fun AnimatedSummaryStrip(uiState: TrackerUiState, analyticsData: AnalyticsData, animFraction: Float) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedSummaryCard("Total saved", "${(uiState.allTimeSaved * animFraction).toInt()} L", Icons.Default.WaterDrop, Color(0xFF1565C0))
        AnimatedSummaryCard("Best day", "${((analyticsData.bestDay?.litresCollected ?: 0.0) * animFraction).toInt()} L", Icons.Default.EmojiEvents, Color(0xFFFFB300))
        AnimatedSummaryCard("Avg/month", "${(analyticsData.avgMonthly * animFraction).toInt()} L", Icons.Default.TrendingUp, Color(0xFF2E7D32))
        SummaryCard("Dry days", "${analyticsData.dryDaysCount}")
    }
}

@Composable
private fun AnimatedSummaryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ImpactHighlights(uiState: TrackerUiState, analyticsData: AnalyticsData, animFraction: Float) {
    val impactDays = calculateImpactDays(uiState.allTimeSaved)
    val waterCostSaved = uiState.allTimeSaved * 0.05 // ₹0.05 per litre (tanker water rate estimate)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${"%.1f".format(impactDays * animFraction)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text("days of supply", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("₹${"%.0f".format(waterCostSaved * animFraction)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                Text("water cost saved", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(uiState.entries.size * animFraction).toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                Text("days tracked", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(132.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthlyCollectionChart(analyticsData: AnalyticsData) {
    val monthly = analyticsData.monthlyTotals.takeLast(12)
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(monthly) {
        if (monthly.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries { series(monthly.map { it.totalLitres }) }
            }
        }
    }
    ChartCard("Monthly Collection", "Litres harvested per month") {
        if (monthly.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom()
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
            MonthLabels(monthly.map { it.monthKey })
            AnimatedColoredBars(monthly)
            VolumeLegend()
        } else {
            Text("No data available for the last 12 months", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RainfallTrendChart(analyticsData: AnalyticsData) {
    val monthly = analyticsData.monthlyTotals.takeLast(12)
    val modelProducer = remember { CartesianChartModelProducer() }
    val average = monthly.map { it.totalMm }.average().takeIf { !it.isNaN() } ?: 0.0
    val max = monthly.maxOfOrNull { it.totalMm }?.coerceAtLeast(1.0) ?: 1.0
    LaunchedEffect(monthly) {
        if (monthly.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(monthly.map { it.totalMm })
                    series(List(monthly.size) { average })
                }
            }
        }
    }
    ChartCard("Rainfall Trend", "Monthly rainfall with average line") {
        if (monthly.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier.matchParentSize()
                )
                Canvas(Modifier.matchParentSize()) {
                    val y = (size.height * (1f - (average / max).toFloat())).coerceIn(0f, size.height)
                    drawLine(
                        color = Color(0xFF757575),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }
            }
            MonthLabels(monthly.map { it.monthKey })
        } else {
            Text("No rainfall data available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun MonthLabels(months: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        months.takeLast(6).forEach {
            Text(
                YearMonth.parse(it).format(DateTimeFormatter.ofPattern("MMM")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VolumeLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem(Color(0xFF2E7D32), "> 2000L")
        LegendItem(Color(0xFF1565C0), "> 1000L")
        LegendItem(Color(0xFFFFB300), "< 1000L")
    }
}

@Composable
private fun AnimatedColoredBars(monthly: List<com.jalsanchay.tracker.util.Calculations.MonthlyTotal>) {
    val max = monthly.maxOfOrNull { it.totalLitres }?.coerceAtLeast(1.0) ?: 1.0
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(monthly) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        monthly.takeLast(12).forEach { item ->
            val color = when {
                item.totalLitres > 2000 -> Color(0xFF2E7D32)
                item.totalLitres > 1000 -> Color(0xFF1565C0)
                else -> Color(0xFFFFB300)
            }
            val targetHeight = (56 * (item.totalLitres / max)).coerceAtLeast(4.0).toFloat()
            Box(
                Modifier
                    .weight(1f)
                    .height((targetHeight * animProgress.value).dp)
                    .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            )
        }
    }
}

@Composable
private fun AnimatedSeasonContribution(analyticsData: AnalyticsData, animFraction: Float) {
    val colors = listOf(Color(0xFF1565C0), Color(0xFF009688), Color(0xFFFFB300), Color(0xFF9E9E9E))
    val entries = analyticsData.seasonBreakdown.entries.toList()
    val total = entries.sumOf { it.value }.coerceAtLeast(1.0)
    ChartCard("Season Contribution", "Share of total harvested water") {
        Canvas(modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally)) {
            var start = -90f
            entries.forEachIndexed { index, entry ->
                val sweep = (entry.value / total * 360f).toFloat() * animFraction
                drawArc(
                    color = colors[index],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(18f, 18f),
                    size = Size(size.width - 36f, size.height - 36f),
                    style = Stroke(width = 28f, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        entries.forEachIndexed { index, entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(12.dp).background(colors[index], RoundedCornerShape(3.dp)))
                Text("${entry.key}: ${"%.1f".format(entry.value / total * 100)}%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PersonalRecords(uiState: TrackerUiState, analyticsData: AnalyticsData) {
    val bestMonth = analyticsData.monthlyTotals.maxByOrNull { it.totalLitres }
    ChartCard("Personal Records", "Your highest collection moments") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordCard("🏆 Best Day", analyticsData.bestDay?.date ?: "-", "${analyticsData.bestDay?.litresCollected?.toInt() ?: 0} L", Modifier.weight(1f))
            RecordCard("📅 Best Month", bestMonth?.displayName ?: "-", "${bestMonth?.totalLitres?.toInt() ?: 0} L", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordCard("🔥 Streak", "${analyticsData.streakDays} days", "consecutive", Modifier.weight(1f))
            RecordCard("📊 Tracked", "${uiState.entries.size} days", "total entries", Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecordCard(title: String, primary: String, secondary: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(secondary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
