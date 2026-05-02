package com.jalsanchay.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Whatshot
import com.jalsanchay.tracker.model.MonthlyReport
import com.jalsanchay.tracker.model.RainfallEntry
import com.jalsanchay.tracker.model.TrackerUiState
import com.jalsanchay.tracker.model.UiState
import com.jalsanchay.tracker.model.UserSettings
import com.jalsanchay.tracker.util.calculateImpactDays
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.round

private object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Dashboard = "dashboard"
    const val Reports = "reports"
    const val History = "history"
    const val Tips = "tips"
    const val Settings = "settings"
    const val Entry = "entry?entryId={entryId}"
    const val Detail = "detail/{entryId}"

    fun entry(entryId: Long = 0L) = "entry?entryId=$entryId"
    fun detail(entryId: Long) = "detail/$entryId"
}

@Composable
fun JalSanchayApp(
    viewModel: TrackerViewModel,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val palette = palette(uiState.settings)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snack.showSnackbar(it) }
    }

    LaunchedEffect(uiState.milestoneMessage) {
        uiState.milestoneMessage?.let { snack.showSnackbar(it) }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = palette.bg) {
            Scaffold(
                containerColor = palette.bg,
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    val route = currentRoute(navController)
                    if (route in mainRoutes) {
                        BottomNav(route, palette) { navController.navigate(it) { launchSingleTop = true } }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.Splash,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Routes.Splash) {
                        SplashScreen(palette)
                        LaunchedEffect(uiState.isLoading, uiState.settings.setupDone) {
                            if (!uiState.isLoading) {
                                delay(1200)
                                navController.navigate(if (uiState.settings.setupDone) Routes.Dashboard else Routes.Onboarding) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(Routes.Onboarding) {
                        OnboardingScreen(uiState.settings, palette) {
                            viewModel.completeSetup(it)
                            navController.navigate(Routes.Dashboard) { popUpTo(Routes.Onboarding) { inclusive = true } }
                        }
                    }
                    composable(Routes.Dashboard) {
                        DashboardScreen(uiState, palette, viewModel) { navController.navigate(Routes.entry()) }
                    }
                    composable(Routes.Reports) {
                        ReportsScreen(uiState, palette, viewModel) {
                            viewModel.exportPdf { file ->
                                scope.launch { snack.showSnackbar("PDF saved: ${file.name}") }
                            }
                        }
                    }
                    composable(Routes.History) {
                        HistoryScreen(
                            entries = uiState.entries,
                            settings = uiState.settings,
                            palette = palette,
                            onDetail = { navController.navigate(Routes.detail(it.id)) },
                            onEdit = { navController.navigate(Routes.entry(it.id)) },
                            onDelete = viewModel::deleteEntry
                        )
                    }
                    composable(Routes.Tips) {
                        TipsScreen(uiState, palette, viewModel)
                    }
                    composable(Routes.Settings) {
                        SettingsScreen(
                            uiState = uiState,
                            palette = palette,
                            onComingSoon = { scope.launch { snack.showSnackbar("Coming in v1.1") } },
                            onSave = {
                                viewModel.saveSettings(it)
                                navController.navigate(Routes.Dashboard) { launchSingleTop = true }
                            },
                            onReset = {
                                viewModel.resetApp()
                                navController.navigate(Routes.Onboarding) {
                                    popUpTo(Routes.Settings) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Routes.Entry,
                        arguments = listOf(navArgument("entryId") { type = NavType.LongType; defaultValue = 0L })
                    ) { backStack ->
                        val id = backStack.arguments?.getLong("entryId") ?: 0L
                        val entry = uiState.entries.firstOrNull { it.id == id }
                        RainfallEntryScreen(
                            settings = uiState.settings,
                            entry = entry,
                            palette = palette,
                            today = viewModel.today(),
                            onBack = { navController.popBackStack() },
                            onSave = { entryId, date, rain ->
                                viewModel.saveRainfallEntry(entryId, date, rain)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = Routes.Detail,
                        arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                    ) { backStack ->
                        val id = backStack.arguments?.getLong("entryId") ?: 0L
                        uiState.entries.firstOrNull { it.id == id }?.let { entry ->
                            RainfallDetailsScreen(
                                entry = entry,
                                palette = palette,
                                onBack = { navController.popBackStack() },
                                onEdit = { navController.navigate(Routes.entry(entry.id)) },
                                onDelete = {
                                    viewModel.deleteEntry(entry)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private val mainRoutes = setOf(Routes.Dashboard, Routes.Reports, Routes.History, Routes.Tips, Routes.Settings)

@Composable
private fun currentRoute(navController: NavController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route
}

private data class AppPalette(
    val bg: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val danger: Color
)

private fun palette(settings: UserSettings): AppPalette {
    return when {
        settings.darkMode && settings.amoledMode -> AppPalette(Color.Black, Color(0xFF080808), Color(0xFF141414), Color.White, Color(0xFFBDBDBD), Color(0xFF80BDFF), Color(0xFFFF8A80))
        settings.darkMode -> AppPalette(Color(0xFF101820), Color(0xFF17212B), Color(0xFF22303D), Color.White, Color(0xFFA8B3C1), Color(0xFF80BDFF), Color(0xFFFF8A80))
        else -> AppPalette(Color(0xFFF0F8FF), Color.White, Color(0xFFF7FBFF), Color(0xFF212121), Color(0xFF667085), Color(0xFF1565C0), Color(0xFFC62828))
    }
}

private data class SeasonInfo(
    val label: String,
    val color: Color,
    val text: String
)

private fun seasonInfo(month: Int): SeasonInfo {
    return when (month) {
        in 3..5 -> SeasonInfo("Pre-Monsoon", Color(0xFFFFC107), "Now is the best time to clean your roof and inspect your tank before the rains arrive.")
        in 6..9 -> SeasonInfo("SW Monsoon", Color(0xFF2196F3), "Log daily during peak monsoon — small amounts add up.")
        in 10..12 -> SeasonInfo("NE Monsoon", Color(0xFF009688), "NE monsoon active. Keep tank covered, diverter clean.")
        else -> SeasonInfo("Dry Season", Color(0xFF9E9E9E), "Dry season. Audit your tank before pre-monsoon.")
    }
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun monthLabel(monthKey: String): String {
    return YearMonth.parse(monthKey).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}

private fun runoffLabel(runoff: Double): String {
    return when (runoff) {
        0.85 -> "Concrete"
        0.75 -> "Tiled"
        0.90 -> "Metal Sheet"
        0.40 -> "Green Roof"
        else -> "Selected"
    }
}

private fun roundOne(value: Double): Double = round(value * 10.0) / 10.0

@Composable
private fun ScreenHeader(title: String, palette: AppPalette, subtitle: String? = null, onBack: (() -> Unit)? = null) {
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
private fun AppCard(palette: AppPalette, modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SplashScreen(palette: AppPalette) {
    Column(Modifier.fillMaxSize().semantics { contentDescription = "Splash screen" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(palette.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Text("J", fontSize = 46.sp, fontWeight = FontWeight.Black, color = palette.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text("Jal-Sanchay Tracker", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = palette.text)
        Text("Every drop counted.", color = palette.muted)
    }
}

@Composable
private fun OnboardingScreen(settings: UserSettings, palette: AppPalette, onDone: (UserSettings) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf(settings) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Setup", palette, "Step ${step + 1} of 3")
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard(palette) {
                when (step) {
                    0 -> {
                        Text("Roof Area", fontWeight = FontWeight.Bold, color = palette.text)
                        OutlinedTextField("${draft.roofArea.toInt()}", { draft = draft.copy(roofArea = it.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth(), label = { Text("Area") })
                        Toggle("sq ft", "sq m", draft.unit == "sqft", palette) { draft = draft.copy(unit = if (it) "sqft" else "sqm") }
                        Text("Typical 3-BHK terrace is about 800-1200 sq ft.", color = palette.muted)
                    }
                    1 -> {
                        Text("Tank Capacity", fontWeight = FontWeight.Bold, color = palette.text)
                        OutlinedTextField("${draft.tankCapacity.toInt()}", { draft = draft.copy(tankCapacity = it.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth(), label = { Text("Litres") })
                    }
                    else -> RunoffSelector(draft.runoffCoeff, palette) { draft = draft.copy(runoffCoeff = it) }
                }
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton({ if (step > 0) step-- }, Modifier.weight(1f)) { Text("Back") }
            Button({ if (step < 2) step++ else onDone(draft) }, Modifier.weight(1f)) { Text(if (step < 2) "Next" else "Start Tracking") }
        }
    }
}

@Composable
private fun DashboardScreen(uiState: TrackerUiState, palette: AppPalette, viewModel: TrackerViewModel, onLog: () -> Unit) {
    val streak by viewModel.streakDays.collectAsStateWithLifecycle()
    val bestDay by viewModel.bestDayLitres.collectAsStateWithLifecycle()
    val averageMonth by viewModel.avgMonthlyLitres.collectAsStateWithLifecycle()
    val dryDays by viewModel.dryDaysCount.collectAsStateWithLifecycle()
    val monthProgress by viewModel.currentMonthProgress.collectAsStateWithLifecycle()
    val distinctMonths by viewModel.distinctMonthCount.collectAsStateWithLifecycle()
    val milestone by viewModel.currentMilestone.collectAsStateWithLifecycle()
    val locationName by viewModel.locationName.collectAsStateWithLifecycle()
    val forecastDays by viewModel.forecastDays.collectAsStateWithLifecycle()
    var forecastExpanded by remember { mutableStateOf(false) }
    var locationDraft by remember { mutableStateOf(locationName) }
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
                AppCard(palette) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = palette.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Milestone reached", color = palette.text, fontWeight = FontWeight.Bold)
                            Text("You have saved ${total.toInt()}L — ${"%.1f".format(calculateImpactDays(total))} days of water supply.", color = palette.muted)
                        }
                        IconButton(onClick = viewModel::dismissMilestone) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss milestone", tint = palette.text)
                        }
                    }
                }
            }
            if (todayLitres == 0.0) {
                AppCard(palette) {
                    Text("Today's harvest", color = palette.text, style = MaterialTheme.typography.titleMedium)
                    Text("Did it rain today?", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
                    Button(onLog) { Text("Log Rainfall") }
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
            if (locationName.isBlank()) {
                TextButton(onClick = { forecastExpanded = !forecastExpanded }) {
                    Text("Set your location for rain forecast →")
                }
                AnimatedVisibility(forecastExpanded) {
                    AppCard(palette) {
                        OutlinedTextField(locationDraft, { locationDraft = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                        Button({ viewModel.setLocationName(locationDraft) }) { Text("Save Location") }
                    }
                }
            } else {
                AppCard(palette) {
                    Text("7-day forecast for $locationName", color = palette.text, fontWeight = FontWeight.Bold)
                    forecastDays.forEach { day ->
                        Text("${day.date}: ${day.precipitationSum.toInt()}mm", color = palette.muted)
                    }
                }
            }
        }
        Button(onLog, Modifier.fillMaxWidth().padding(16.dp).semantics { contentDescription = "Log rainfall" }) { Text("Log Rainfall") }
    }
}

@Composable
private fun StatColumn(label: String, value: String, palette: AppPalette, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = palette.muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WaterTank(percent: Double, palette: AppPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(58.dp).height(112.dp).background(palette.surfaceStrong, RoundedCornerShape(14.dp)).semantics { contentDescription = "Tank ${(percent * 100).toInt()} percent full" }, contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().height((112f * percent.toFloat()).dp).background(palette.primary, RoundedCornerShape(12.dp)))
        }
        Text("Tank ${(percent * 100).toInt()}%", color = palette.muted, fontSize = 12.sp)
    }
}

@Composable
private fun SummaryCard(label: String, value: String, palette: AppPalette, modifier: Modifier) {
    AppCard(palette, modifier) {
        Text(label, color = palette.muted)
        Text(value, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun RainfallEntryScreen(settings: UserSettings, entry: RainfallEntry?, palette: AppPalette, today: String, onBack: () -> Unit, onSave: (Long, String, Double) -> Unit) {
    var date by remember { mutableStateOf(entry?.date ?: today) }
    var rain by remember { mutableStateOf(entry?.rainfallMm?.toString() ?: "") }
    val value = rain.toDoubleOrNull()
    val error = rain.isBlank() || value == null || value < 0.0

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (entry == null) "Log Rainfall" else "Edit Entry", palette, onBack = onBack)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard(palette) {
                OutlinedTextField(date, { date = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Date") })
                Text("Backdated entry allowed up to 7 days.", color = palette.muted)
            }
            AppCard(palette) {
                OutlinedTextField(rain, { rain = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Rainfall in mm") })
                if (error) Text("Enter a non-negative rainfall value.", color = palette.danger)
                Text("Calculation caps collection at ${settings.tankCapacity.toInt()} L tank capacity.", color = palette.muted)
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onBack, Modifier.weight(1f)) { Text("Cancel") }
            Button({ if (!error) onSave(entry?.id ?: 0L, date, value!!) }, Modifier.weight(1f), enabled = !error) { Text("Save") }
        }
    }
}

@Composable
private fun ReportsScreen(uiState: TrackerUiState, palette: AppPalette, viewModel: TrackerViewModel, onExportPdf: () -> Unit) {
    val bestMonth by viewModel.bestMonth.collectAsStateWithLifecycle()
    val previousMonthLitres by viewModel.previousMonthLitres.collectAsStateWithLifecycle()
    val aiSeasonInsight by viewModel.aiSeasonInsight.collectAsStateWithLifecycle()
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

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Monthly Reports", palette)
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
                    Text(it.monthKey, color = palette.text, fontWeight = FontWeight.Bold)
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
                Button(onClick = viewModel::fetchSeasonAnalysis) { Text("Get AI Insight") }
                when (val state = aiSeasonInsight) {
                    UiState.Idle -> Unit
                    UiState.Loading -> CircularProgressIndicator()
                    is UiState.Success -> Text(state.value, color = palette.muted)
                    is UiState.Error -> Text("Unable to generate response. Tap to retry.", color = palette.danger)
                }
            }
        }
        Button(onExportPdf, Modifier.fillMaxWidth().padding(16.dp)) { Text("Export PDF") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(entries: List<RainfallEntry>, settings: UserSettings, palette: AppPalette, onDetail: (RainfallEntry) -> Unit, onEdit: (RainfallEntry) -> Unit, onDelete: (RainfallEntry) -> Unit) {
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
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${filtered.size} entries · ${"%.1f".format(totalMm)}mm · ${totalLitres.toInt()}L", color = palette.muted)
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
            if (filtered.isEmpty()) AppCard(palette) { Text("No records yet", color = palette.text, fontWeight = FontWeight.Bold); Text("Add your first rainfall entry", color = palette.muted) }
            filtered.forEachIndexed { index, entry ->
                val previous = entries.getOrNull(entries.indexOfFirst { it.id == entry.id } + 1)
                val trend = when {
                    previous == null -> "first record"
                    entry.rainfallMm > previous.rainfallMm -> "↑"
                    entry.rainfallMm < previous.rainfallMm -> "↓"
                    else -> "same"
                }
                AppCard(palette, Modifier.clickable { expandedId = if (expandedId == entry.id) null else entry.id }.semantics { contentDescription = "Rainfall ${entry.rainfallMm} mm on ${entry.date}" }) {
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
                        Text("Edit", color = palette.primary, modifier = Modifier.clickable { onEdit(entry) }.padding(8.dp))
                        Text("Delete", color = palette.danger, modifier = Modifier.clickable { confirmDelete = entry }.padding(8.dp))
                    }
                    AnimatedVisibility(expandedId == entry.id) {
                        Text(
                            "${entry.rainfallMm}mm × ${settings.roofArea.toInt()} ${settings.unit} × 0.0929 × ${settings.runoffCoeff} (${runoffLabel(settings.runoffCoeff)}) = ${entry.litresCollected.toInt()}L" +
                                if (entry.litresCollected == settings.tankCapacity) " · Capped at ${settings.tankCapacity.toInt()}L" else "",
                            color = palette.muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
private fun RainfallDetailsScreen(entry: RainfallEntry, palette: AppPalette, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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

@Composable
private fun TipsScreen(uiState: TrackerUiState, palette: AppPalette, viewModel: TrackerViewModel) {
    var open by remember { mutableIntStateOf(0) }
    var openedTips by remember { mutableStateOf(setOf<Int>()) }
    var question by remember { mutableStateOf("") }
    var qaHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val aiTips by viewModel.aiTips.collectAsStateWithLifecycle()
    val season = seasonInfo(LocalDate.now().monthValue)
    LaunchedEffect(Unit) {
        viewModel.glossaryAnswer.collect { pair ->
            qaHistory = qaHistory + pair
        }
    }
    val tips = listOf(
        "Clean your roof before monsoon" to "Remove leaves, dust, and debris before heavy rain.",
        "Install a first-flush diverter" to "Divert initial dirty rainwater away from your tank.",
        "Check tank for cracks before season" to "Inspect joints, outlet, overflow, and lid.",
        "Use ferro-cement or food-grade plastic" to "These materials are durable for household storage.",
        "Cover your tank" to "Reduce mosquito breeding and prevent debris entry.",
        "Understand runoff coefficient" to "It is the fraction of rainfall that reaches storage.",
        "Inspect gutters monthly" to "Blocked gutters reduce collection efficiency.",
        "Keep overflow safe" to "Route overflow to garden or recharge pits.",
        "Use a mesh filter" to "Filter leaves and insects before storage.",
        "Record zero-rain days" to "It keeps reports accurate."
    )
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tips & Education", palette)
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = palette.primary)
                    Text(season.text, color = palette.text, modifier = Modifier.padding(start = 12.dp))
                }
            }
            AppCard(palette) {
                Text("You have explored ${openedTips.size} of 10 tips", color = palette.muted)
                LinearProgressIndicator(progress = { openedTips.size / 10f }, modifier = Modifier.fillMaxWidth())
            }
            tips.forEachIndexed { index, tip ->
                AppCard(palette, Modifier.clickable {
                    open = if (open == index) -1 else index
                    openedTips = openedTips + index
                }.animateContentSize()) {
                    Text(tip.first, color = palette.text, fontWeight = FontWeight.Bold)
                    AnimatedVisibility(open == index) { Text(tip.second, color = palette.muted) }
                }
            }
            AppCard(palette) {
                Text("AI Personalised Tips", color = palette.text, fontWeight = FontWeight.Bold)
                Button(onClick = viewModel::fetchTips) { Text("Get AI Tips") }
                when (val state = aiTips) {
                    UiState.Idle -> Unit
                    UiState.Loading -> CircularProgressIndicator()
                    is UiState.Success -> Text(state.value, color = palette.muted)
                    is UiState.Error -> Text(state.message, color = palette.danger)
                }
            }
            AppCard(palette) {
                Text("Glossary", color = palette.text, fontWeight = FontWeight.Bold)
                Text("Runoff Coefficient, Catchment Area, First Flush, Water Harvesting Potential, CPHEEO Standard", color = palette.muted)
                OutlinedTextField(question, { question = it }, label = { Text("Ask anything about rainwater harvesting") }, modifier = Modifier.fillMaxWidth())
                Button({ if (question.isNotBlank()) { viewModel.askGlossary(question); question = "" } }) { Text("Ask") }
                qaHistory.forEach { pair ->
                    Text("Q: ${pair.first}", color = palette.primary, fontWeight = FontWeight.Bold)
                    Text(pair.second, color = palette.text)
                }
            }
        }
    }
}

private enum class SaveState { IDLE, SAVED }

@Composable
private fun SettingsScreen(uiState: TrackerUiState, palette: AppPalette, onComingSoon: () -> Unit, onSave: (UserSettings) -> Unit, onReset: () -> Unit) {
    val settings = uiState.settings
    var draft by remember(settings) { mutableStateOf(settings) }
    var confirmReset by remember { mutableStateOf(false) }
    var saveState by remember { mutableStateOf(SaveState.IDLE) }
    var pendingSave by remember { mutableStateOf(false) }
    LaunchedEffect(saveState) {
        if (saveState == SaveState.SAVED) {
            delay(1500)
            if (pendingSave) {
                pendingSave = false
                onSave(draft)
            }
            saveState = SaveState.IDLE
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", palette)
        Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard(palette) {
                Text("Roof Area", color = palette.text, fontWeight = FontWeight.Bold)
                OutlinedTextField("${draft.roofArea.toInt()}", { draft = draft.copy(roofArea = it.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth())
                Toggle("sq ft", "sq m", draft.unit == "sqft", palette) { draft = draft.copy(unit = if (it) "sqft" else "sqm") }
            }
            AppCard(palette) {
                Text("Tank Capacity (L)", color = palette.text, fontWeight = FontWeight.Bold)
                OutlinedTextField("${draft.tankCapacity.toInt()}", { draft = draft.copy(tankCapacity = it.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth())
            }
            AppCard(palette) { RunoffSelector(draft.runoffCoeff, palette) { draft = draft.copy(runoffCoeff = it) } }
            AppCard(palette) {
                Text("Appearance", color = palette.text, fontWeight = FontWeight.Bold)
                Toggle("Light", "Dark", !draft.darkMode, palette) { draft = draft.copy(darkMode = !it) }
                if (draft.darkMode) Toggle("Standard", "AMOLED", !draft.amoledMode, palette) { draft = draft.copy(amoledMode = !it) }
            }
            AppCard(palette) {
                Text("Rainfall reminders", color = palette.text, fontWeight = FontWeight.Bold)
                Toggle("Off", "On", !draft.rainfallReminderEnabled, palette) { draft = draft.copy(rainfallReminderEnabled = !it) }
                Text("Schedules a local daily reminder. No backend required.", color = palette.muted)
            }
            AppCard(palette) {
                Text("Notifications", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                ListItem(
                    headlineContent = { Text("Rain day reminders") },
                    trailingContent = { Switch(checked = false, onCheckedChange = null, enabled = false) },
                    modifier = Modifier.clickable { onComingSoon() }
                )
                ListItem(
                    headlineContent = { Text("Milestone alerts") },
                    trailingContent = { Switch(checked = false, onCheckedChange = null, enabled = false) },
                    modifier = Modifier.clickable { onComingSoon() }
                )
            }
            AppCard(palette) {
                Text("About", color = palette.text, fontWeight = FontWeight.Bold)
                Text("App version v1.0. CPHEEO reference: 135 L per person per day.", color = palette.muted)
                ListItem(
                    headlineContent = { Text("Database") },
                    supportingContent = { Text("${uiState.entries.size} entries · ${roundOne(uiState.entries.size * 0.15)}KB") }
                )
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({
                pendingSave = true
                saveState = SaveState.SAVED
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
private fun RunoffSelector(selected: Double, palette: AppPalette, onSelect: (Double) -> Unit) {
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
private fun Toggle(left: String, right: String, leftSelected: Boolean, palette: AppPalette, onChange: (Boolean) -> Unit) {
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

@Composable
private fun BottomNav(route: String?, palette: AppPalette, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = palette.surface) {
        listOf(
            Routes.Dashboard to "Home",
            Routes.Reports to "Reports",
            Routes.History to "History",
            Routes.Tips to "Tips",
            Routes.Settings to "Settings"
        ).forEach { item ->
            NavigationBarItem(
                selected = route == item.first,
                onClick = { onSelect(item.first) },
                icon = { Text("[]", color = if (route == item.first) palette.primary else palette.muted) },
                label = { Text(item.second) }
            )
        }
    }
}
