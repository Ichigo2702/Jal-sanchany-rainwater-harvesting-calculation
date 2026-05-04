package com.jalsanchay.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jalsanchay.tracker.model.RainfallEntry
import com.jalsanchay.tracker.model.UserSettings
import com.jalsanchay.tracker.util.Calculations
import com.jalsanchay.tracker.util.calculateImpactDays

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    entries: List<RainfallEntry>,
    settings: UserSettings,
    palette: AppPalette,
    onDetail: (RainfallEntry) -> Unit,
    onEdit: (RainfallEntry) -> Unit,
    onDelete: (RainfallEntry) -> Unit
) {
    var confirmDelete by remember { mutableStateOf<RainfallEntry?>(null) }
    var expandedMenu by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val months = entries.map { it.date.take(7) }.distinct().sortedDescending()
    val filtered = selectedMonth?.let { month -> entries.filter { it.date.startsWith(month) } } ?: entries
    val totalMm = filtered.sumOf { it.rainfallMm }
    val totalLitres = filtered.sumOf { it.litresCollected }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rainfall History", palette)
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("${filtered.size} entries · ${"%.1f".format(totalMm)}mm · ${totalLitres.toInt()}L", color = palette.muted)
            }
            item {
                ExposedDropdownMenuBox(expanded = expandedMenu, onExpandedChange = { expandedMenu = !expandedMenu }) {
                    OutlinedTextField(
                        value = selectedMonth?.let { monthLabel(it) } ?: "All entries",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        DropdownMenuItem(text = { Text("All entries") }, onClick = { selectedMonth = null; expandedMenu = false })
                        months.forEach { month ->
                            DropdownMenuItem(text = { Text(monthLabel(month)) }, onClick = { selectedMonth = month; expandedMenu = false })
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    AppCard(palette) {
                        Text("No records yet", color = palette.text, fontWeight = FontWeight.Bold)
                        Text("Add your first rainfall entry", color = palette.muted)
                    }
                }
            }
            items(filtered, key = { it.id }) { entry ->
                val previous = entries.getOrNull(entries.indexOfFirst { it.id == entry.id } + 1)
                val trend = when {
                    previous == null -> "first record"
                    entry.rainfallMm > previous.rainfallMm -> "↑"
                    entry.rainfallMm < previous.rainfallMm -> "↓"
                    else -> "same"
                }
                HistoryEntryCard(
                    entry = entry,
                    settings = settings,
                    palette = palette,
                    trend = trend,
                    isExpanded = expandedId == entry.id.toLong(),
                    onTap = { expandedId = if (expandedId == entry.id.toLong()) null else entry.id.toLong() },
                    onEdit = { onEdit(entry) },
                    onDelete = { confirmDelete = entry }
                )
            }
        }
    }
    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("This record will be removed from history.") },
            confirmButton = { TextButton({ onDelete(entry); confirmDelete = null }) { Text("Yes") } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: RainfallEntry,
    settings: UserSettings,
    palette: AppPalette,
    trend: String,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    AppCard(
        palette,
        Modifier
            .clickable { onTap() }
            .animateContentSize()
            .semantics { contentDescription = "Rainfall ${entry.rainfallMm} mm on ${entry.date}" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${entry.rainfallMm.toInt()} mm", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.text)
                Text("${entry.litresCollected.toInt()} L", color = palette.muted)
                Text(entry.date, color = palette.muted)
            }
            Text(
                trend,
                color = when (trend) {
                    "↑" -> Color(0xFF2E7D32)
                    "↓" -> Color(0xFFC62828)
                    else -> palette.muted
                },
                modifier = Modifier.padding(8.dp)
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = palette.muted)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = palette.danger) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
        AnimatedVisibility(isExpanded) {
            Text(
                "${entry.rainfallMm}mm × ${settings.roofArea.toInt()} ${settings.unit} × ${Calculations.RUNOFF_FACTOR} × ${settings.runoffCoeff} (${runoffLabel(settings.runoffCoeff)}) = ${entry.litresCollected.toInt()}L" +
                    if (entry.litresCollected == settings.tankCapacity) " · Capped at ${settings.tankCapacity.toInt()}L" else "",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun RainfallDetailsScreen(entry: RainfallEntry, palette: AppPalette, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rainfall Details", palette, entry.date, onBack)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard(palette) {
                Text("${entry.rainfallMm.toInt()} mm", fontSize = 42.sp, fontWeight = FontWeight.Black, color = palette.text)
                Text("${entry.litresCollected.toInt()} L collected", color = palette.muted)
                Text("${"%.1f".format(calculateImpactDays(entry.litresCollected))} days of water supply", color = palette.muted)
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onEdit, Modifier.weight(1f)) { Text("Edit") }
            OutlinedButton(onDelete, Modifier.weight(1f)) { Text("Delete", color = palette.danger) }
        }
    }
}
