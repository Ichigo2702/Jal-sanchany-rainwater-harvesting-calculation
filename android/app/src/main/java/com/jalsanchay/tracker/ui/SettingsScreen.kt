package com.jalsanchay.tracker.ui

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.model.UserSettings
import com.jalsanchay.tracker.util.validateRoofArea
import com.jalsanchay.tracker.util.validateTankCapacity
import com.jalsanchay.tracker.util.isValid
import com.jalsanchay.tracker.util.errorMessage
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import kotlinx.coroutines.delay

private enum class SaveState { IDLE, SAVED }

@Composable
internal fun SettingsScreen(
    uiState: TrackerUiState,
    palette: AppPalette,
    viewModel: TrackerViewModel,
    onComingSoon: () -> Unit,
    onSave: (UserSettings) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val settings = uiState.settings
    var draft by remember(settings) { mutableStateOf(settings) }
    var roofInput by remember(settings.roofArea) { mutableStateOf(settings.roofArea.toInt().toString()) }
    var tankInput by remember(settings.tankCapacity) { mutableStateOf(settings.tankCapacity.toInt().toString()) }
    var roofTouched by remember { mutableStateOf(false) }
    var tankTouched by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var saveState by remember { mutableStateOf(SaveState.IDLE) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importData(context, it) }
    }
    val roofValidation = validateRoofArea(roofInput, draft.unit)
    val tankValidation = validateTankCapacity(tankInput)

    // Location state
    val locationName by viewModel.locationName.collectAsStateWithLifecycle()
    val weatherLoading by viewModel.weatherLoading.collectAsStateWithLifecycle()
    var locationDraft by remember(locationName) { mutableStateOf(locationName) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.detectLocation(context)
    }

    // Export data share intent
    val exportedDataUri by viewModel.exportedDataUri.collectAsStateWithLifecycle()
    val exportShareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    LaunchedEffect(saveState) {
        if (saveState == SaveState.SAVED) {
            delay(800)
            saveState = SaveState.IDLE
        }
    }

    LaunchedEffect(exportedDataUri) {
        exportedDataUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            exportShareLauncher.launch(Intent.createChooser(shareIntent, "Export Jal-Sanchay backup"))
            viewModel.clearExportedDataUri()
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", palette)
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── Location ──
            SectionHeader("Location", Icons.Default.LocationOn, palette)
            AppCard(palette) {
                Text(if (locationName.isBlank()) "Not set" else "📍 $locationName", color = palette.muted)
                if (weatherLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Fetching weather...", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        locationDraft,
                        { locationDraft = it },
                        label = { Text("City name") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Detect location", tint = palette.primary)
                    }
                }
                Button(
                    onClick = { viewModel.setLocationName(locationDraft) },
                    enabled = locationDraft.isNotBlank() && !weatherLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (weatherLoading) "Saving..." else "Save Location") }
            }

            // ── Setup ──
            SectionHeader("Roof & Tank", Icons.Default.Straighten, palette)
            AppCard(palette) {
                Text("Roof Area", color = palette.text, fontWeight = FontWeight.Bold)
                OutlinedTextField(roofInput, { roofInput = it; roofTouched = true }, modifier = Modifier.fillMaxWidth())
                if (roofTouched && !roofValidation.isValid) ValidationText(roofValidation.errorMessage.orEmpty())
                Toggle("sq ft", "sq m", draft.unit == "sqft", palette) { draft = draft.copy(unit = if (it) "sqft" else "sqm") }
            }
            AppCard(palette) {
                Text("Tank Capacity (L)", color = palette.text, fontWeight = FontWeight.Bold)
                OutlinedTextField(tankInput, { tankInput = it; tankTouched = true }, modifier = Modifier.fillMaxWidth())
                if (tankTouched && !tankValidation.isValid) ValidationText(tankValidation.errorMessage.orEmpty())
            }
            AppCard(palette) { RunoffSelector(draft.runoffCoeff, palette) { draft = draft.copy(runoffCoeff = it) } }

            // ── Appearance ──
            SectionHeader("Appearance", Icons.Default.DarkMode, palette)
            AppCard(palette) {
                Toggle("Light", "Dark", !draft.darkMode, palette) { draft = draft.copy(darkMode = !it) }
                if (draft.darkMode) {
                    Spacer(Modifier.height(4.dp))
                    Toggle("Standard", "AMOLED", !draft.amoledMode, palette) { draft = draft.copy(amoledMode = !it) }
                }
            }

            // ── Notifications ──
            SectionHeader("Notifications", Icons.Default.Notifications, palette)
            AppCard(palette) {
                Text("Rainfall reminders", color = palette.text, fontWeight = FontWeight.Bold)
                Toggle("Off", "On", !draft.rainfallReminderEnabled, palette) { draft = draft.copy(rainfallReminderEnabled = !it) }
                Text("Daily reminder to check & log rainfall.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = { Text("Rain day reminders") },
                    supportingContent = { Text("Coming in v1.1", color = palette.muted) },
                    trailingContent = { Switch(checked = false, onCheckedChange = null, enabled = false) },
                    modifier = Modifier.clickable { onComingSoon() }
                )
                ListItem(
                    headlineContent = { Text("Milestone alerts") },
                    supportingContent = { Text("Coming in v1.1", color = palette.muted) },
                    trailingContent = { Switch(checked = false, onCheckedChange = null, enabled = false) },
                    modifier = Modifier.clickable { onComingSoon() }
                )
            }

            // ── Data ──
            SectionHeader("Data", Icons.Default.CloudUpload, palette)
            AppCard(palette) {
                OutlinedButton({
                    viewModel.exportData(context)
                }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.padding(start = 8.dp))
                    Text("Export Data")
                }
                OutlinedButton({
                    importLauncher.launch(arrayOf("application/json", "text/*"))
                }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.padding(start = 8.dp))
                    Text("Import Data")
                }
                Text("${uiState.entries.size} entries · ${roundOne(uiState.entries.size * 0.15)}KB", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }

            // ── About ──
            SectionHeader("About", Icons.Default.Info, palette)
            AppCard(palette) {
                Text("Jal-Sanchay Tracker", color = palette.text, fontWeight = FontWeight.Bold)
                Text("Version 1.0 • Built with ❤️ for water conservation", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("CPHEEO reference: 135 L per person per day", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                Text("Database: ${uiState.entries.size} entries", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({
                roofTouched = true
                tankTouched = true
                if (roofValidation.isValid && tankValidation.isValid) {
                    draft = draft.copy(roofArea = roofInput.toDouble(), tankCapacity = tankInput.toDouble())
                    onSave(draft)
                    saveState = SaveState.SAVED
                }
            }, Modifier.fillMaxWidth()) {
                AnimatedContent(saveState, label = "save-state") { state ->
                    if (state == SaveState.SAVED) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                            Text("Saved!", color = Color(0xFF2E7D32), modifier = Modifier.padding(start = 6.dp))
                        }
                    } else {
                        Text("Save Changes")
                    }
                }
            }
            OutlinedButton({ confirmReset = true }, Modifier.fillMaxWidth()) { Text("Reset App", color = palette.danger) }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("This will delete all data. Continue?") },
            text = { Text("Setup values and rainfall history will be cleared.") },
            confirmButton = { TextButton({ confirmReset = false; onReset() }) { Text("Reset") } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, palette: AppPalette) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
        Text(title, color = palette.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
    }
}
