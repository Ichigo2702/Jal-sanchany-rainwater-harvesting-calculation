# Jal-Sanchay Project Workflow & Architecture

This document provides a comprehensive overview of how the Jal-Sanchay Rainwater Harvesting Tracker works, detailing its workflows, data flows, and file structures for both the React and Android implementations.

---

##  Core Workflow & How it Works

The application (both React and Android) operates on the following core lifecycle:
1. **Onboarding / Setup**: The user enters their household specifics: Roof Area, Unit (sq ft/sq m), Tank Capacity, and Roof Type (determines the runoff coefficient).
2. **Dashboard**: Acts as the central hub. It displays the current month's progress, streak, total water saved, AI-generated insights, and short-term rainfall forecasts.
3. **Logging Rainfall**: Users input the amount of rainfall (in mm) for a specific date.
4. **Calculations**: The app uses the formula `Rainfall (mm) × Roof Area (sqm) × Runoff Coefficient = Water Collected (Litres)`. If the calculated water exceeds the tank capacity, it is capped.
5. **History & Reports**: Users can view past entries, edit/delete them, and see monthly performance charts.
6. **AI Insights**: The app integrates with Anthropic's Claude API to provide personalized tips, season analysis, and a glossary for rainwater harvesting terms.

---

##  React Web Application

The web version is built using React, TypeScript, and Vite. It relies on a local `useReducer` for state management and simulates a mobile-like shell interface.

### React File Structure

```text
src/
├── components/          # UI Screens and reusable components
│   ├── DashboardScreen.tsx  # Main hub, summary stats, AI tips
│   ├── HistoryScreen.tsx    # List of past rainfall entries
│   ├── Onboarding.tsx       # Initial setup form
│   ├── RainfallEntryScreen.tsx # Form to log/edit rainfall
│   ├── ReportsScreen.tsx    # Monthly charts and CSV export
│   ├── SettingsScreen.tsx   # Edit roof/tank setup, import/export data
│   ├── TipsScreen.tsx       # AI season analysis and detailed tips
│   └── UI.tsx               # Shared UI elements (Dialogs, Toasts)
├── services/            # Business logic and external API calls
│   ├── aiService.ts         # Prompts and fetch calls to Anthropic Claude API
│   └── calculations.ts      # Core mathematical logic for runoff and capacity
├── App.tsx              # Root component: manages global state via useReducer & handles routing
├── index.css            # Global styling, theming (dark/light/amoled), and utility classes
├── main.tsx             # Entry point mounting the React tree
├── mockData.ts          # Seed data for initial testing and demonstration
└── types.ts             # Global TypeScript interfaces (AppState, Entry, etc.)
```

### React Data Flow
1. **State Management**: `App.tsx` uses `useReducer` to maintain `AppState` (settings, entries, theme).
2. **Routing**: Instead of an external router like `react-router`, `App.tsx` conditionally renders components based on a `screen` state variable (e.g., `"dashboard"`, `"history"`).
3. **Persistence**: The web app uses JSON file imports/exports (`exportJson`, `importJson`) and CSV exports for data management.
4. **Recalculation**: If a user changes their roof size or tank capacity in Settings, `App.tsx` intercepts the `patch` action and triggers `recalculateEntries` to retroactively update all past data based on the new constraints.

---

##  Android Native Application

The Android version is built natively using Kotlin, Jetpack Compose, Room Database, and Coroutines/Flows. It provides a more robust, persistent, and OS-integrated experience.

### Android File Structure

```text
android/app/src/main/java/com/jalsanchay/tracker/
├── MainActivity.kt      # Android entry point, sets up the Compose content
├── JalSanchayApp.kt     # (in ui/) Main Compose shell, handles bottom navigation and routing
├── ai/
│   └── AiTipService.kt  # Claude API integration using Ktor/Retrofit equivalents
├── data/                # Local persistence layer
│   ├── Dao.kt           # Room Data Access Object (SQL queries)
│   ├── Entities.kt      # Room table definitions (UserSettings, RainfallEntry)
│   ├── JalSanchayDatabase.kt # Room DB configuration
│   └── TrackerRepository.kt  # Single source of truth abstracting the DB operations
├── model/               # Kotlin data classes representing the domain (UiState, Forecast)
├── notifications/       # OS Integrations
│   ├── ReminderReceiver.kt   # BroadcastReceiver triggered by alarms
│   └── ReminderScheduler.kt  # Uses AlarmManager to schedule daily logging reminders
├── util/                # Helper functions
│   ├── PdfExporter.kt   # Generates PDF reports using Android's PdfDocument
│   └── Calculations.kt  # Core math for rainwater harvesting (same logic as React)
└── viewmodel/
    └── TrackerViewModel.kt # Connects Repository data to the UI using StateFlows
```

### Android Data Flow
1. **State Management**: `TrackerViewModel.kt` acts as the brain. It observes the `TrackerRepository` via Kotlin `Flow`s. It uses `combine` to merge user settings and rainfall entries into a unified `TrackerUiState`.
2. **Persistence Layer**: Data is permanently stored in an SQLite database using Room (`Dao.kt`, `Entities.kt`). Any changes (inserts/updates) automatically emit new Flow values to the ViewModel.
3. **UI Reactivity**: The Compose UI (`JalSanchayApp.kt` and subsequent screens) collects the `StateFlow` from the ViewModel. When the DB updates, the UI recomposes automatically.
4. **OS Features**:
   - **Reminders**: The user can enable daily reminders in settings, which uses the Android `AlarmManager` to trigger a local notification to log rain.
   - **PDF Export**: Unlike the web's CSV/JSON approach, Android utilizes native canvas drawing (`PdfExporter.kt`) to generate and share rich PDF reports. 
