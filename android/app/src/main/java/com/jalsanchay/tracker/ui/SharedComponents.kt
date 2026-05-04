package com.jalsanchay.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jalsanchay.tracker.model.UserSettings
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.round

// ── Data classes ──

internal data class AppPalette(
    val bg: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val danger: Color
)

internal data class SeasonInfo(
    val label: String,
    val color: Color,
    val text: String
)

// ── Utility functions ──

internal fun palette(settings: UserSettings): AppPalette {
    return when {
        settings.darkMode && settings.amoledMode -> AppPalette(Color.Black, Color(0xFF080808), Color(0xFF141414), Color.White, Color(0xFFBDBDBD), Color(0xFF80BDFF), Color(0xFFFF8A80))
        settings.darkMode -> AppPalette(Color(0xFF101820), Color(0xFF17212B), Color(0xFF22303D), Color.White, Color(0xFFA8B3C1), Color(0xFF80BDFF), Color(0xFFFF8A80))
        else -> AppPalette(Color(0xFFF0F8FF), Color.White, Color(0xFFF7FBFF), Color(0xFF212121), Color(0xFF667085), Color(0xFF1565C0), Color(0xFFC62828))
    }
}

internal fun seasonInfo(month: Int): SeasonInfo {
    return when (month) {
        in 3..5 -> SeasonInfo("Pre-Monsoon", Color(0xFFFFC107), "Now is the best time to clean your roof and inspect your tank before the rains arrive.")
        in 6..9 -> SeasonInfo("SW Monsoon", Color(0xFF2196F3), "Log daily during peak monsoon — small amounts add up.")
        in 10..12 -> SeasonInfo("NE Monsoon", Color(0xFF009688), "NE monsoon active. Keep tank covered, diverter clean.")
        else -> SeasonInfo("Dry Season", Color(0xFF9E9E9E), "Dry season. Audit your tank before pre-monsoon.")
    }
}

internal fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

internal fun monthLabel(monthKey: String): String {
    return YearMonth.parse(monthKey).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}

internal fun runoffLabel(runoff: Double): String {
    return when (runoff) {
        0.85 -> "Concrete"
        0.75 -> "Tiled"
        0.90 -> "Metal Sheet"
        0.40 -> "Green Roof"
        else -> "Selected"
    }
}

internal fun roundOne(value: Double): Double = round(value * 10.0) / 10.0

// ── Shared composables ──

@Composable
internal fun ScreenHeader(title: String, palette: AppPalette, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().background(palette.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Text(
                "<",
                fontSize = 32.sp,
                color = palette.text,
                modifier = Modifier
                    .clickable(onClickLabel = "Go back") { onBack() }
                    .padding(end = 12.dp)
                    .semantics { contentDescription = "Back" }
            )
        }
        Column {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = palette.text)
            if (subtitle != null) Text(subtitle, color = palette.muted)
        }
    }
}

@Composable
internal fun AppCard(palette: AppPalette, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun StatColumn(label: String, value: String, palette: AppPalette, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = palette.muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun WaterTank(percent: Double, palette: AppPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(58.dp).height(112.dp).background(palette.surfaceStrong, RoundedCornerShape(14.dp)).semantics { contentDescription = "Tank ${(percent * 100).toInt()} percent full" }, contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().height((112f * percent.toFloat()).dp).background(palette.primary, RoundedCornerShape(12.dp)))
        }
        Text("Tank ${(percent * 100).toInt()}%", color = palette.muted, fontSize = 12.sp)
    }
}

@Composable
internal fun SummaryCard(label: String, value: String, palette: AppPalette, modifier: Modifier = Modifier) {
    AppCard(palette, modifier) {
        Text(label, color = palette.muted)
        Text(value, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
internal fun ValidationText(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
    )
}

@Composable
internal fun RunoffSelector(selected: Double, palette: AppPalette, onSelect: (Double) -> Unit) {
    Text("Runoff Coefficient", color = palette.text, fontWeight = FontWeight.Bold)
    Text("Runoff coefficient = % of rainwater collected from roof", color = palette.muted)
    listOf("Concrete" to 0.85, "Tiled" to 0.75, "Metal Sheet" to 0.90, "Green Roof" to 0.40).forEach {
        Text(
            "${it.first} (${it.second})",
            color = if (selected == it.second) palette.primary else palette.text,
            fontWeight = if (selected == it.second) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth().clickable { onSelect(it.second) }.padding(vertical = 10.dp)
        )
    }
}

@Composable
internal fun Toggle(left: String, right: String, leftSelected: Boolean, palette: AppPalette, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.surfaceStrong).padding(4.dp)) {
        Text(
            left,
            color = if (leftSelected) palette.primary else palette.muted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (leftSelected) palette.surface else Color.Transparent).clickable { onChange(true) }.padding(12.dp)
        )
        Text(
            right,
            color = if (!leftSelected) palette.primary else palette.muted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (!leftSelected) palette.surface else Color.Transparent).clickable { onChange(false) }.padding(12.dp)
        )
    }
}
