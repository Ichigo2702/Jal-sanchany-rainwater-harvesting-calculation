# Jal-Sanchay Project Workflow & Architecture

This document provides a comprehensive overview of how the Jal-Sanchay Rainwater Harvesting Tracker works, detailing its workflows, data flows, and file structures for both the React and Android implementations.

**Last updated:** May 2026 (v1.0 production-ready)

---

## Core Workflow & How it Works

The application (both React and Android) operates on the following core lifecycle:
1. **Onboarding / Setup**: The user enters their household specifics: Roof Area, Unit (sq ft/sq m), Tank Capacity, and Roof Type (determines the runoff coefficient). The Android version features a step-by-step wizard with progress dots, icons, and preset values.
2. **Dashboard**: Acts as the central hub. It displays the current month's progress, streak, total water saved, milestone celebrations, and short-term rainfall forecasts. Users can set/change their location for weather integration.
3. **Logging Rainfall**: Users input the amount of rainfall (in mm) for a specific date. Backdated entries are allowed up to 7 days.
4. **Calculations**: The app uses the formula `Rainfall (mm) × Roof Area (sqm) × Runoff Coefficient = Water Collected (Litres)`. If the calculated water exceeds the tank capacity, it is capped.
5. **History & Reports**: Users can view past entries with trend indicators (↑/↓), edit/delete them via dropdown menus, filter by month, and see monthly performance charts with readable month names.
6. **Analytics**: Animated charts showing monthly collection trends, rainfall patterns, season contribution (donut chart), personal records, and water cost savings in ₹.
7. **AI Insights**: The app integrates with Anthropic's Claude API to provide personalized tips, season analysis, and a glossary for rainwater harvesting terms.
8. **Backup & Restore**: Full JSON export/import with share intent support, including all settings (theme, location, setup values).

---

## React Web Application

The web version is built using React, TypeScript, and Vite. It relies on a local `useReducer` for state management and simulates a mobile-like shell interface.

### React File Structure

```text
src/
├── components/          # UI Screens and reusable components
│   ├── DashboardScreen.tsx  # Main hub, summary stats, AI tips
│   ├── HistoryScreen.tsx    # List of past rainfall entries
│   ├── Onboarding.tsx       # Initial setup form
│   ├── RainfallDetailScreen.tsx # Screen showing details of a specific rainfall entry
│   ├── RainfallEntryScreen.tsx # Form to log/edit rainfall
│   ├── ReportsScreen.tsx    # Monthly charts and CSV export
│   ├── SettingsScreen.tsx   # Edit roof/tank setup, import/export data
│   ├── SplashScreen.tsx     # Splash screen shown during initial app load
│   ├── TipsScreen.tsx       # AI season analysis and detailed tips
│   └── UI.tsx               # Shared UI elements (Dialogs, Toasts)
├── services/            # Business logic and external API calls
│   ├── aiService.ts         # Prompts and fetch calls to Anthropic Claude API
│   └── calculations.ts      # Core mathematical logic for runoff and capacity
├── App.tsx              # Root component: manages global state via useReducer & handles routing
├── index.css            # Global styling, theming (dark/light/amoled), and utility classes
├── main.tsx             # Entry point mounting the React tree
├── initialState.ts      # Empty frontend initial state; seed data lives in Android Room backend
└── types.ts             # Global TypeScript interfaces (AppState, Entry, etc.)
```

### React Data Flow
1. **State Management**: `App.tsx` uses `useReducer` to maintain `AppState` (settings, entries, theme).
2. **Routing**: Instead of an external router like `react-router`, `App.tsx` conditionally renders components based on a `screen` state variable (e.g., `"dashboard"`, `"history"`).
3. **Persistence**: The web app uses JSON file imports/exports (`exportJson`, `importJson`) and CSV exports for data management.
4. **Recalculation**: If a user changes their roof size or tank capacity in Settings, `App.tsx` intercepts the `patch` action and triggers `recalculateEntries` to retroactively update all past data based on the new constraints.

---

## Android Native Application

The Android version is built natively using Kotlin, Jetpack Compose, Room Database, and Coroutines/Flows. It follows a modular screen-based architecture with a shared ViewModel.

### Android File Structure

```text
android/app/src/main/java/com/jalsanchay/tracker/
├── MainActivity.kt          # Android entry point, sets up Compose content & work scheduling
├── ui/                      # Modular screen-based UI layer
│   ├── JalSanchayApp.kt        # Main Compose shell: navigation, bottom bar, onboarding, splash
│   ├── DashboardScreen.kt      # Home screen: stats, tank visual, milestones, weather forecast
│   ├── HistoryScreen.kt        # LazyColumn list with dropdown actions, month filter
│   ├── ReportsScreen.kt        # Year-by-year monthly reports, AI season analysis, PDF export
│   ├── AnalyticsScreen.kt      # Animated charts: monthly bars, rainfall trend, season donut, records
│   ├── TipsScreen.kt           # 10 expandable tips, AI tips, glossary Q&A
│   ├── SettingsScreen.kt       # Sectioned settings: location, setup, theme, notifications, data, about
│   ├── CalculatorScreen.kt     # Interactive rainfall calculator with slider and live preview
│   └── SharedComponents.kt     # AppPalette, AppCard, WaterTank, Toggle, ScreenHeader, etc.
├── ai/
│   └── AiTipService.kt     # Claude API integration with rate limiting and response caching
├── data/                    # Local persistence layer
│   ├── Dao.kt               # Room DAOs: UserSetupDao, RainfallDao, WeatherDao
│   ├── Entities.kt          # Room entities: UserSetup, RainfallEntry, WeatherCache
│   ├── JalSanchayDatabase.kt   # Room DB config with Migration 1→2 (added theme/reminder columns)
│   ├── SeedData.kt          # 80+ realistic seed entries spanning May 2025–May 2026
│   └── TrackerRepository.kt    # Single source of truth: DB abstraction, weather cache, recalculation
├── model/
│   └── Models.kt            # TrackerUiState, AnalyticsData, UiState sealed class, type aliases
├── notifications/           # OS Integrations
│   ├── RainPredictionWorker.kt  # Background WorkManager task for rain prediction alerts
│   ├── ReminderReceiver.kt      # BroadcastReceiver triggered by alarms
│   └── ReminderScheduler.kt    # Uses AlarmManager to schedule daily logging reminders
├── util/                    # Helper functions
│   ├── Calculations.kt     # Core math: water collected, impact score, streak, monthly totals
│   ├── DataBackup.kt       # JSON export/import with FileProvider URI sharing
│   ├── LocationHelper.kt   # getCurrentLocation() with lastLocation fallback
│   ├── NetworkMonitor.kt   # ConnectivityManager-based online/offline detection
│   ├── PdfExporter.kt      # Generates PDF reports using Android's PdfDocument
│   ├── Validators.kt       # Input validation for roof area, tank capacity, rainfall, dates
│   ├── WeatherFetcher.kt   # Open-Meteo API integration for weather data
│   └── WeatherSyncWorker.kt # Background worker to sync weather data
├── viewmodel/
│   └── TrackerViewModel.kt     # Connects Repository → DerivedStats → UI via StateFlows
├── widget/
│   ├── JalSanchayWidget.kt     # Glance AppWidget for home screen quick stats
│   └── JalSanchayWidgetReceiver.kt # Receiver for the Glance AppWidget
```

### Android Data Flow
1. **State Management**: `TrackerViewModel.kt` acts as the brain. It observes the `TrackerRepository` via Kotlin `Flow`s. It computes a consolidated `DerivedStats` object from entries in a single `map` operation (instead of 8 separate flows), then exposes individual `StateFlow`s for each metric. These are combined into a unified `TrackerUiState` using Kotlin's `combine`.
2. **Persistence Layer**: Data is permanently stored in an SQLite database using Room (`Dao.kt`, `Entities.kt`). Any changes (inserts/updates) automatically emit new Flow values to the ViewModel.
3. **UI Reactivity**: The Compose UI screens collect `StateFlow`s from the ViewModel using `collectAsStateWithLifecycle()`. When the DB updates, only affected parts of the UI recompose.
4. **OS Features**:
   - **Reminders**: The user can enable daily reminders in settings, which uses the Android `AlarmManager` to trigger a local notification to log rain.
   - **PDF Export**: Utilizes native canvas drawing (`PdfExporter.kt`) to generate and share rich PDF reports.
   - **Widget**: Glance AppWidget displays quick stats on the home screen.
   - **Background Work**: `RainPredictionWorker` checks forecast data and sends notifications when rain is expected.

### Key Architecture Decisions
- **Modular screens**: Extracted from a monolithic 1200-line `JalSanchayApp.kt` into 7 focused screen files.
- **Consolidated StateFlows**: All entry-derived stats (streak, best day, monthly totals, etc.) are computed once in `DerivedStats` instead of 8 separate `_entries.map{}` chains — significantly reducing recomposition.
- **Milestone system**: Dismissing uses an unconditional set-based approach to prevent re-showing lower milestones.
- **Theme**: Supports Light, Dark, and AMOLED modes via `AppPalette` (runtime palette object, not Material theme override).
- **Location**: Uses `getCurrentLocation()` with `lastLocation` fallback; editable after initial save.
- **Weather**: Open-Meteo API with 3-hour cache in Room, 7-day forecast display.
- **Seed data**: 80+ entries covering all Indian rainfall seasons, computed via `Calculations.calculateWaterCollected()` for consistency.
