package com.jalsanchay.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.jalsanchay.tracker.model.RainfallEntry
import com.jalsanchay.tracker.model.UserSettings
import com.jalsanchay.tracker.util.isValid
import com.jalsanchay.tracker.util.errorMessage
import com.jalsanchay.tracker.util.message
import com.jalsanchay.tracker.util.validateDate
import com.jalsanchay.tracker.util.validateRainfallMm
import com.jalsanchay.tracker.util.validateRoofArea
import com.jalsanchay.tracker.util.validateTankCapacity
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Dashboard = "dashboard"
    const val Reports = "reports"
    const val Analytics = "analytics"
    const val History = "history"
    const val Tips = "tips"
    const val Settings = "settings"
    const val Calculator = "calculator"
    const val Entry = "entry?entryId={entryId}&rainMm={rainMm}"
    const val Detail = "detail/{entryId}"

    fun entry(entryId: Long = 0L, rainMm: Double = -1.0) = "entry?entryId=$entryId&rainMm=$rainMm"
    fun detail(entryId: Long) = "detail/$entryId"
}

private val mainRoutes = setOf(Routes.Dashboard, Routes.Reports, Routes.Analytics, Routes.History, Routes.Tips, Routes.Settings)

@Composable
fun JalSanchayApp(
    viewModel: TrackerViewModel,
    navController: NavHostController,
    deepLinkScreen: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appPalette = palette(uiState.settings)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snack.showSnackbar(it) }
    }

    LaunchedEffect(uiState.milestoneMessage) {
        uiState.milestoneMessage?.let { snack.showSnackbar(it) }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snack.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(deepLinkScreen) {
        if (deepLinkScreen == "entry") {
            navController.navigate(Routes.Dashboard) { launchSingleTop = true }
            navController.navigate(Routes.entry())
        } else if (deepLinkScreen == "dashboard") {
            navController.navigate(Routes.Dashboard) { launchSingleTop = true }
        }
    }

    val colorScheme = remember(appPalette) {
        if (uiState.settings.darkMode) {
            darkColorScheme(
                primary = appPalette.primary,
                background = appPalette.bg,
                surface = appPalette.surface,
                onPrimary = Color.White,
                onBackground = appPalette.text,
                onSurface = appPalette.text,
                error = appPalette.danger
            )
        } else {
            lightColorScheme(
                primary = appPalette.primary,
                background = appPalette.bg,
                surface = appPalette.surface,
                onPrimary = Color.White,
                onBackground = appPalette.text,
                onSurface = appPalette.text,
                error = appPalette.danger
            )
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(Modifier.fillMaxSize(), color = appPalette.bg) {
            Scaffold(
                containerColor = appPalette.bg,
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    val route = currentRoute(navController)
                    if (route in mainRoutes) {
                        BottomNav(route, appPalette) { navController.navigate(it) { launchSingleTop = true } }
                    }
                }
            ) { padding ->
                Column(Modifier.padding(padding)) {
                    AnimatedVisibility(
                        visible = !uiState.isOnline,
                        enter = slideInVertically(),
                        exit = slideOutVertically()
                    ) {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WifiOff, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Offline — weather and AI features unavailable", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Splash,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(Routes.Splash) {
                            SplashScreen(appPalette)
                            LaunchedEffect(uiState.isLoading, uiState.settings.setupDone) {
                                if (!uiState.isLoading) {
                                    delay(500)
                                    navController.navigate(if (uiState.settings.setupDone) Routes.Dashboard else Routes.Onboarding) {
                                        popUpTo(Routes.Splash) { inclusive = true }
                                    }
                                }
                            }
                        }
                        composable(Routes.Onboarding) {
                            OnboardingScreen(uiState.settings, appPalette) {
                                viewModel.completeSetup(it)
                                navController.navigate(Routes.Dashboard) { popUpTo(Routes.Onboarding) { inclusive = true } }
                            }
                        }
                        composable(Routes.Dashboard) {
                            DashboardScreen(
                                uiState = uiState,
                                palette = appPalette,
                                viewModel = viewModel,
                                onLog = { navController.navigate(Routes.entry()) },
                                onCalculator = { navController.navigate(Routes.Calculator) },
                                onPermissionDenied = { scope.launch { snack.showSnackbar("Location permission needed") } }
                            )
                        }
                        composable(Routes.Reports) {
                            ReportsScreen(uiState, appPalette, viewModel) {
                                navController.navigate(Routes.Calculator)
                            }
                        }
                        composable(Routes.Analytics) {
                            val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()
                            AnalyticsScreen(uiState, analyticsData)
                        }
                        composable(Routes.History) {
                            HistoryScreen(
                                entries = uiState.entries,
                                settings = uiState.settings,
                                palette = appPalette,
                                onDetail = { navController.navigate(Routes.detail(it.id.toLong())) },
                                onEdit = { navController.navigate(Routes.entry(it.id.toLong())) },
                                onDelete = viewModel::deleteEntry
                            )
                        }
                        composable(Routes.Tips) {
                            TipsScreen(uiState, appPalette, viewModel)
                        }
                        composable(Routes.Settings) {
                            SettingsScreen(
                                uiState = uiState,
                                palette = appPalette,
                                viewModel = viewModel,
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
                        composable(Routes.Calculator) {
                            CalculatorScreen(
                                setup = uiState.settings,
                                onBack = { navController.popBackStack() },
                                onLogToday = { rainMm ->
                                    navController.navigate(Routes.entry(rainMm = rainMm))
                                }
                            )
                        }
                        composable(
                            route = Routes.Entry,
                            arguments = listOf(
                                navArgument("entryId") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("rainMm") { type = NavType.FloatType; defaultValue = -1f }
                            )
                        ) { backStack ->
                            val id = backStack.arguments?.getLong("entryId") ?: 0L
                            val rainMm = backStack.arguments?.getFloat("rainMm")?.takeIf { it >= 0f }?.toDouble()
                            val entry = uiState.entries.firstOrNull { it.id.toLong() == id }
                            RainfallEntryScreen(
                                settings = uiState.settings,
                                entry = entry,
                                initialRainMm = rainMm,
                                palette = appPalette,
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
                            uiState.entries.firstOrNull { it.id.toLong() == id }?.let { entry ->
                                RainfallDetailsScreen(
                                    entry = entry,
                                    palette = appPalette,
                                    onBack = { navController.popBackStack() },
                                    onEdit = { navController.navigate(Routes.entry(entry.id.toLong())) },
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
}

@Composable
private fun currentRoute(navController: NavController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route
}

@Composable
private fun SplashScreen(palette: AppPalette) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
    var roofInput by remember(settings.roofArea) { mutableStateOf(settings.roofArea.toInt().toString()) }
    var tankInput by remember(settings.tankCapacity) { mutableStateOf(settings.tankCapacity.toInt().toString()) }
    var roofTouched by remember { mutableStateOf(false) }
    var tankTouched by remember { mutableStateOf(false) }
    val roofValidation = validateRoofArea(roofInput, draft.unit)
    val tankValidation = validateTankCapacity(tankInput)
    val stepIcons = listOf(
        Icons.Default.Home,
        Icons.Default.Settings,
        Icons.Default.PieChart
    )
    val stepTitles = listOf("Roof Area", "Tank Capacity", "Roof Type")
    val stepSubtitles = listOf(
        "How large is your rainwater catchment area?",
        "What is your storage tank capacity?",
        "What material is your roof made of?"
    )

    Column(Modifier.fillMaxSize()) {
        // Header with gradient
        Column(
            Modifier.fillMaxWidth().background(palette.surface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Jal-Sanchay", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.text)
            Spacer(Modifier.height(4.dp))
            Text("Let's set up your rainwater tracker", color = palette.muted)
            Spacer(Modifier.height(20.dp))
            // Step progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                (0..2).forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == step) 12.dp else 8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (i <= step) palette.primary else palette.muted.copy(alpha = 0.3f))
                    )
                }
            }
        }
        Column(Modifier.weight(1f).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Step icon + title
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(palette.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(stepIcons[step], contentDescription = null, tint = palette.primary, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(stepTitles[step], fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.text)
                Spacer(Modifier.height(4.dp))
                Text(stepSubtitles[step], color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            }
            // Step content
            AppCard(palette) {
                when (step) {
                    0 -> {
                        OutlinedTextField(roofInput, { roofInput = it; roofTouched = true }, modifier = Modifier.fillMaxWidth(), label = { Text("Area") })
                        if (roofTouched && !roofValidation.isValid) ValidationText(roofValidation.errorMessage.orEmpty())
                        Toggle("sq ft", "sq m", draft.unit == "sqft", palette) { draft = draft.copy(unit = if (it) "sqft" else "sqm") }
                        Spacer(Modifier.height(8.dp))
                        Text("💡 Typical 3-BHK terrace: 800–1200 sq ft", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    1 -> {
                        OutlinedTextField(tankInput, { tankInput = it; tankTouched = true }, modifier = Modifier.fillMaxWidth(), label = { Text("Litres") })
                        if (tankTouched && !tankValidation.isValid) ValidationText(tankValidation.errorMessage.orEmpty())
                        Spacer(Modifier.height(8.dp))
                        Text("💡 Common sizes: 1000L, 2000L, 5000L, 10000L", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1000", "2000", "5000").forEach { preset ->
                                OutlinedButton(
                                    onClick = { tankInput = preset; tankTouched = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(preset) }
                            }
                        }
                    }
                    else -> RunoffSelector(draft.runoffCoeff, palette) { draft = draft.copy(runoffCoeff = it) }
                }
            }
        }
        // Step indicator text + buttons
        Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp)) {
            Text("Step ${step + 1} of 3", color = palette.muted, style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { if (step > 0) step-- },
                modifier = Modifier.weight(1f),
                enabled = step > 0,
                shape = RoundedCornerShape(14.dp)
            ) { Text("Back") }
            Button(
                onClick = {
                    when (step) {
                        0 -> {
                            roofTouched = true
                            if (roofValidation.isValid) {
                                draft = draft.copy(roofArea = roofInput.toDouble())
                                step++
                            }
                        }
                        1 -> {
                            tankTouched = true
                            if (tankValidation.isValid) {
                                draft = draft.copy(tankCapacity = tankInput.toDouble())
                                step++
                            }
                        }
                        else -> onDone(draft.copy(roofArea = roofInput.toDouble(), tankCapacity = tankInput.toDouble()))
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (step < 2) "Next →" else "🚀 Start Tracking") }
        }
    }
}

@Composable
private fun RainfallEntryScreen(settings: UserSettings, entry: RainfallEntry?, initialRainMm: Double?, palette: AppPalette, today: String, onBack: () -> Unit, onSave: (Long, String, Double) -> Unit) {
    var date by remember { mutableStateOf(entry?.date ?: today) }
    var rain by remember { mutableStateOf(entry?.rainfallMm?.toString() ?: initialRainMm?.toInt()?.toString().orEmpty()) }
    var dateTouched by remember { mutableStateOf(false) }
    var rainTouched by remember { mutableStateOf(false) }
    val value = rain.toDoubleOrNull()
    val dateValidation = validateDate(date)
    val rainValidation = validateRainfallMm(rain)
    val error = !dateValidation.isValid || !rainValidation.isValid

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (entry == null) "Log Rainfall" else "Edit Entry", palette, onBack = onBack)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard(palette) {
                OutlinedTextField(date, { date = it }, modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.hasFocus) dateTouched = true }, label = { Text("Date") })
                if (dateTouched && !dateValidation.isValid) Text(dateValidation.message.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Backdated entry allowed up to 7 days.", color = palette.muted)
            }
            AppCard(palette) {
                OutlinedTextField(rain, { rain = it }, modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.hasFocus) rainTouched = true }, label = { Text("Rainfall in mm") })
                if (rainTouched && !rainValidation.isValid) Text(rainValidation.message.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Calculation caps collection at ${settings.tankCapacity.toInt()} L tank capacity.", color = palette.muted)
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onBack, Modifier.weight(1f)) { Text("Cancel") }
            Button({
                dateTouched = true
                rainTouched = true
                if (!error) onSave(entry?.id?.toLong() ?: 0L, date, value!!)
            }, Modifier.weight(1f)) { Text("Save") }
        }
    }
}

@Composable
private fun BottomNav(route: String?, palette: AppPalette, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = palette.surface) {
        listOf(
            Triple(Routes.Dashboard, "Home", Icons.Default.Home),
            Triple(Routes.Reports, "Reports", Icons.Default.BarChart),
            Triple(Routes.Analytics, "Analytics", Icons.Default.PieChart),
            Triple(Routes.History, "History", Icons.Default.History),
            Triple(Routes.Tips, "Tips", Icons.Default.Lightbulb),
            Triple(Routes.Settings, "Settings", Icons.Default.Settings)
        ).forEach { (path, label, icon) ->
            NavigationBarItem(
                selected = route == path,
                onClick = { onSelect(path) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.primary,
                    selectedTextColor = palette.primary,
                    unselectedIconColor = palette.muted,
                    unselectedTextColor = palette.muted,
                    indicatorColor = palette.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
