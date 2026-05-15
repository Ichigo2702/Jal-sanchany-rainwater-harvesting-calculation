# 💧 Jal-Sanchay Tracker

> **A native Android app for rainwater harvesting calculation and tracking — built for Indian households.**

Jal-Sanchay Tracker helps homeowners estimate, log, and optimise their rainwater collection. Enter your roof dimensions, local rainfall data, and tank capacity, and the app automatically calculates how much water you can harvest, tracks daily readings, and gives AI-powered conservation tips — all stored locally on your device with no cloud dependency.

---

## 📋 Table of Contents

- [Problem Statement](#-problem-statement)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Project Structure](#-project-structure)
- [Setup & Installation](#-setup--installation)
- [Usage Guide](#-usage-guide)
- [Screenshots](#-screenshots)
- [Future Improvements](#-future-improvements)
- [Contributing](#-contributing)

---

## 🎯 Problem Statement

Rainwater harvesting is underutilised in urban India largely because residents have no easy way to estimate potential yield, monitor collection in real time, or understand when and how much water to expect. Jal-Sanchay Tracker solves this by putting a full calculation, logging, and analytics suite directly in a smartphone.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM with Coroutines & StateFlow |
| **Local Database** | Room (SQLite) |
| **Background Work** | WorkManager + AlarmManager |
| **Networking** | OkHttp (Open-Meteo weather API) |
| **Charts** | Vico (Compose charts library) |
| **Location** | Google Play Services Location |
| **Reports** | Native Android `PdfDocument` API |
| **Home Widget** | Jetpack Glance (AppWidget) |
| **AI Tips** | Gemini API (via `local.properties` key) |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | API 36 |

---

## ✨ Features

### 🌧 Rainwater Calculator
- Calculates harvestable volume from roof area, rainfall depth, and runoff coefficient.
- Supports multiple roof types (flat, sloped, RCC).
- Instant results with formula breakdown shown on-screen.

### 📊 Dashboard & Analytics
- Daily, weekly, and monthly collection summaries.
- Interactive charts powered by Vico showing rainfall trends.
- Tank fill-level indicator with visual progress.

### 📅 History Log
- Chronological log of every manual or auto-synced rainfall reading.
- Edit or delete individual entries.
- Export full history to a PDF report.

### 🌦 Weather Integration
- Fetches live rainfall forecast from the **Open-Meteo API** (no key required).
- GPS-based automatic location detection.
- Background sync via WorkManager; forecast cached to avoid repeated requests.

### 🔔 Smart Reminders
- Scheduled local notifications reminding users to log their daily reading.
- Uses `AlarmManager` with exact alarm support (Android 12+).
- Boot-aware receiver restores alarms after device restart.

### 📄 PDF Report Generation
- Generates a formatted PDF summary of collection data.
- Opens with any installed PDF viewer via `FileProvider`.

### 🤖 AI Conservation Tips
- Gemini-powered tips screen suggests personalised water-saving actions.
- Falls back gracefully when offline or when no API key is configured.

### 🏠 Home Screen Widget
- Glance-based widget showing today's tank level and last recorded reading.
- Auto-updates whenever the underlying data changes.

### 💾 Data Backup & Restore
- Export/import full database as a JSON file for backup.
- Share backup via any app using Android's share sheet.

### ⚙️ Settings
- Configure tank capacity, roof area, and runoff coefficient.
- Choose reminder time and frequency.
- Toggle metric/imperial units.
- Dark / Light / System theme.

---

## 📁 Project Structure

```
android/
├── app/
│   ├── build.gradle.kts          # App-level Gradle config (dependencies, SDK versions)
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, activities, receivers, providers
│       └── java/com/jalsanchay/tracker/
│           ├── MainActivity.kt           # Single-activity entry point
│           ├── ai/
│           │   └── AiTipService.kt       # Gemini API integration
│           ├── data/
│           │   ├── Dao.kt                # Room DAO queries
│           │   ├── Entities.kt           # Room entity definitions
│           │   ├── JalSanchayDatabase.kt # Room database builder
│           │   ├── SeedData.kt           # Prepopulated reference data
│           │   └── TrackerRepository.kt  # Single source of truth for data ops
│           ├── model/
│           │   └── Models.kt             # Domain data classes
│           ├── notifications/
│           │   ├── RainPredictionWorker.kt  # WorkManager worker for forecast sync
│           │   ├── ReminderReceiver.kt      # BroadcastReceiver for alarms
│           │   └── ReminderScheduler.kt     # Alarm scheduling helper
│           ├── ui/
│           │   ├── JalSanchayApp.kt      # NavHost & bottom navigation
│           │   ├── DashboardScreen.kt    # Home dashboard
│           │   ├── CalculatorScreen.kt   # Rainwater calculator
│           │   ├── AnalyticsScreen.kt    # Charts & analytics
│           │   ├── HistoryScreen.kt      # Collection log
│           │   ├── ReportsScreen.kt      # PDF report generation
│           │   ├── SettingsScreen.kt     # User preferences
│           │   ├── TipsScreen.kt         # AI tips
│           │   └── SharedComponents.kt   # Reusable Compose composables
│           ├── util/
│           │   ├── Calculations.kt       # Core harvesting formulas
│           │   ├── DataBackup.kt         # JSON export/import
│           │   ├── LocationHelper.kt     # GPS location wrapper
│           │   ├── NetworkMonitor.kt     # Connectivity checks
│           │   ├── PdfExporter.kt        # PDF generation logic
│           │   ├── Validators.kt         # Input validation helpers
│           │   ├── WeatherFetcher.kt     # Open-Meteo API client
│           │   └── WeatherSyncWorker.kt  # Periodic weather sync worker
│           ├── viewmodel/
│           │   └── TrackerViewModel.kt   # Central ViewModel (StateFlow state)
│           └── widget/
│               ├── JalSanchayWidget.kt           # Glance widget UI
│               └── JalSanchayWidgetReceiver.kt   # Widget update receiver
├── build.gradle.kts              # Project-level Gradle config
├── gradle.properties             # JVM & Gradle flags
├── settings.gradle.kts           # Module & plugin management declarations
└── local.properties              # SDK path & API keys (gitignored)
```

---

## ⚙️ Setup & Installation

### Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | API 26–36 installed |
| Gradle | Managed by wrapper (`./gradlew`) |

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Ichigo2702/Jal-sanchany-rainwater-harvesting-calculation.git
   cd Jal-sanchany-rainwater-harvesting-calculation
   ```

2. **Open in Android Studio**
   - Launch **Android Studio**.
   - Choose **File → Open** and navigate to the `android/` folder inside the cloned repo.
   - Wait for Gradle sync to complete automatically.

3. **Configure the Gemini API key** *(optional — AI Tips screen only)*
   - Open (or create) `android/local.properties`.
   - Add the following line:
     ```properties
     GEMINI_API_KEY=your_gemini_api_key_here
     ```
   - The file is already gitignored; your key will never be committed.
   - The app works fully without a key; the Tips screen will simply show a placeholder message.

4. **Run the app**
   - Connect a physical device **or** start an Android emulator (API 26+).
   - Click the **▶ Run** button in Android Studio, or use the terminal:
     ```bash
     cd android
     ./gradlew installDebug
     ```

5. **Build a release APK** *(optional)*
   ```bash
   cd android
   ./gradlew assembleRelease
   ```
   The output APK is located at `app/build/outputs/apk/release/`.

---

## 📖 Usage Guide

1. **First Launch** — The app opens the Dashboard showing an empty tank and today's weather (if location permission is granted).
2. **Set Up Your Profile** — Go to **Settings** and enter your roof area (m²), tank capacity (L), and runoff coefficient.
3. **Calculate Potential Yield** — Open the **Calculator** tab, enter a rainfall amount (or let the weather sync fill it in), and tap **Calculate**.
4. **Log a Reading** — From the Dashboard, tap the **+ Add Reading** button to manually record how much water was collected.
5. **View Analytics** — The **Analytics** tab shows bar/line charts of your collection over days, weeks, or months.
6. **Generate a PDF Report** — Tap **Reports → Export PDF** to create a shareable PDF of your collection history.
7. **Enable Reminders** — In Settings, turn on daily reminders so you never forget to log a reading.
8. **Add the Widget** — Long-press your home screen, select **Widgets**, and add the Jal-Sanchay widget for a quick tank level glance.

---

## 📸 Screenshots

### 🏠 Home Dashboard
The Dashboard is the central hub of the app. It greets you with a time-aware message (Good morning / afternoon / evening) alongside the current date and an Indian rainfall season badge (e.g., **Pre-Monsoon**). The **Today's harvest** card lets you log rainfall or mark a dry day in a single tap. Below it, summary tiles show **This Month's** total harvested volume, your current logging **streak**, and key all-time stats — best single-day yield (Best Day), monthly average (Avg/Month), and total dry days. A horizontal progress bar tracks the current month's performance against the historical monthly average. A shortcut link lets you set your GPS location to enable the 7-day weather forecast.

![Home Dashboard](screenshots/home%20dashboard%201.png)

---

### 🧙 Onboarding — Step 1: Roof Area
The guided 3-step setup wizard begins by collecting your roof's catchment area — the surface from which rainwater is harvested. You can enter a custom value and toggle between **sq ft** and **sq m**. A contextual hint ("Typical 3-BHK terrace: 800–1200 sq ft") helps first-time users pick a realistic number. Progress dots at the top show position in the wizard (Step 1 of 3).

![Onboarding – Roof Area](screenshots/setup%201.png)

---

### 🧙 Onboarding — Step 2: Tank Capacity
Step 2 captures the capacity of your underground or overhead storage tank in **litres**. Preset quick-select buttons (1000 L, 2000 L, 5000 L) cover the most common tank sizes sold in India, while the free-text field accepts any custom value. A hint lists common sizes (1000L, 2000L, 5000L, 10000L) for reference. This value is used to cap the daily harvest calculation — water collected beyond tank capacity is not counted.

![Onboarding – Tank Capacity](screenshots/setup%202.png)

---

### 🧙 Onboarding — Step 3: Roof Type & Runoff Coefficient
The final setup step determines your **runoff coefficient** — the fraction of rainfall that actually flows off your roof into the collection system. Four roof types are offered: **Concrete (0.85)**, **Tiled (0.75)**, **Metal Sheet (0.90)**, and **Green Roof (0.40)**. A brief explanation clarifies what the coefficient means. The selected option is highlighted in blue. Tapping **🚀 Start Tracking** saves all three values to the Room database and launches the main app.

![Onboarding – Roof Type](screenshots/setup%203.png)

---

### 📊 Analytics — Charts & Insights
The Analytics screen visualises your long-term collection data. Three hero stat cards at the top display **Total saved** (104,935 L), **Best day** (3,000 L), and **Avg/month** (8,071 L). A highlighted banner shows three impact metrics in colour-coded text: days of water supply (777.3 days in blue), estimated water cost saved in ₹ (₹5,247 in green), and total days tracked (80 days in orange). Below, a **Monthly Collection** bar chart powered by the Vico library shows per-month harvest volumes from December through May, with colour-coded bars indicating yield level (green > 2000 L, blue > 1000 L, yellow < 1000 L). A **Rainfall Trend** section follows below.

![Analytics](screenshots/analytics%201.png)

---

### 📅 Rainfall History
The History screen provides a reverse-chronological log of every rainfall entry. A subtitle shows lifetime totals at a glance (80 entries · 1466.0 mm · 104,935 L). A dropdown filter allows viewing all entries or filtering by month. Each entry card displays the **rainfall in mm**, **water harvested in litres**, and the **date**. A coloured trend arrow (↑ green / ↓ red) compares the reading against the previous entry. A three-dot overflow menu on each card provides **Edit** and **Delete** actions, allowing backdated corrections of up to 7 days.

![Rainfall History](screenshots/history%201.png)

---

### 📄 Monthly Reports
The Reports screen offers a year-by-year summary of collection data, navigable with back/forward arrows. An **All-time summary** card at the top aggregates lifetime figures: total rainfall (1466.0 mm), total water saved (104,935 L), equivalent days of supply (777.3 days), and months with data (13 months). Below, individual month cards list rainfall depth, litres saved, and equivalent supply days, each accompanied by a progress bar showing relative performance. Tapping **Export PDF Report** generates a formatted PDF using Android's native `PdfDocument` API and opens a share sheet to save or send it.

![Monthly Reports](screenshots/reports%201.png)

---

### ⚙️ Settings — Location & Roof/Tank Configuration
The top section of Settings manages **Location** for weather integration. Users can type a city name or tap the pin icon to auto-detect GPS coordinates, then tap **Save Location** to persist it. The **Roof & Tank** section below lets users update their roof area (with sq ft / sq m toggle) and tank capacity at any time — changes trigger a retroactive recalculation of all historical entries. A **Save Changes** button commits updates; **Reset App** wipes all data and returns to onboarding.

![Settings – Location & Roof/Tank](screenshots/settings%201.png)

---

### ⚙️ Settings — Appearance & Notifications
Scrolling further in Settings reveals the **Appearance** section, where users switch between **Light** and **Dark** themes. The **Notifications** section controls daily **Rainfall reminders** (Off/On toggle with an explanatory subtitle). Additional notification types — Rain day reminders and Milestone alerts — are shown as coming in v1.1, giving users a preview of upcoming features. All changes are saved with the same **Save Changes** button.

![Settings – Appearance & Notifications](screenshots/settings%202.png)

---

### 💡 Tips & Education — Overview
The Tips & Education screen is the app's knowledge hub. The top portion shows expandable **best-practice tip cards** (Inspect gutters monthly, Keep overflow safe, Use a mesh filter, Record zero-rain days). Below the tips list, an **AI Personalised Tips** card with a **Get AI Tips** button fetches context-aware advice from the Gemini API based on the user's setup and season. At the bottom, a **Glossary** section lists key rainwater harvesting terms (Runoff Coefficient, Catchment Area, First Flush, Water Harvesting Potential, CPHEEO Standard) and an **Ask anything** free-text field for on-demand Q&A.

![Tips & Education – Overview](screenshots/tips%20and%20glossary%201.png)

---

### 💡 Tips & Education — Expanded Tips List
When the Tips screen loads with data, a seasonal context banner appears at the top (e.g., *"Now is the best time to clean your roof and inspect your tank before the rains arrive."*). A progress tracker ("You have explored 0 of 10 tips") with a progress bar motivates users to read all tips. Each tip is displayed as a tappable accordion card with a title and a short description visible on expansion — for example: **Clean your roof before monsoon** (Remove leaves, dust, and debris before heavy rain), **Install a first-flush diverter**, **Check tank for cracks before season**, **Use ferro-cement or food-grade plastic**, **Cover your tank**, and **Understand runoff coefficient**.

![Tips – Expanded List](screenshots/tips%20and%20glossary%202.png)

---

### 💡 Tips & Education — AI Glossary Q&A
The Glossary section at the bottom of the Tips screen doubles as an AI-powered Q&A interface. Users type any question about rainwater harvesting into the text field and tap **Ask**. The Gemini API returns a personalised answer grounded in the user's own setup data — as shown here, a question *"best way to clean the roof?"* returns a tailored response referencing the user's 800 sq ft concrete roof in India, with step-by-step cleaning instructions. When the AI service is temporarily unavailable, a graceful fallback message is shown ("Google AI service is temporarily down. Try again later.").

![Tips – AI Glossary Q&A](screenshots/tips%20and%20glossary%203.png)

---

## 🚀 Future Improvements

The following features are planned for upcoming releases:

| Feature | Target Version | Description |
|---|---|---|
| **Rain day reminders** | v1.1 | Smart push notifications triggered by weather forecast when rain is expected |
| **Milestone alerts** | v1.1 | Celebrate water-saving milestones (e.g., 10,000 L saved) with in-app and push notifications |
| **Multi-tank support** | v1.2 | Allow users to manage multiple storage tanks with individual tracking |
| **Offline AI tips** | v1.2 | Bundled local tip engine for users without internet access |
| **Community benchmarks** | v1.3 | Compare your household harvest against anonymised city-level averages |
| **Water bill calculator** | v1.3 | Input your local water tariff and see real INR savings per month |
| **Dark / AMOLED theme** | v1.1 | Full dark mode and AMOLED-optimised black background theme |
| **CSV export** | v1.2 | Export rainfall history as a CSV file for spreadsheet analysis |
| **Wear OS widget** | v2.0 | Quick glance at tank level from a smartwatch |

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository.
2. Create your feature branch: `git checkout -b feature/amazing-feature`.
3. Commit your changes: `git commit -m 'Add amazing feature'`.
4. Push to the branch: `git push origin feature/amazing-feature`.
5. Open a Pull Request.

---

## 📄 License

This project is for educational and personal use. All rights reserved by the author.

---

<p align="center">Made with ❤️ for sustainable water management in India 💧</p>
