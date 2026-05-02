# Jal-Sanchay Tracker

Jal-Sanchay Tracker is a dedicated rainwater harvesting calculation and tracking application designed specifically for Indian households. It aims to help users estimate, track, and optimize their rainwater collection based on their roof area, rainfall, and tank capacity. 

The project contains two distinct frontends that share the same core logic and purpose:
1. A **React** web application (Progressive Web App style).
2. A native **Android** application using Jetpack Compose.

##  Tech Stack

### React Web App
- **Framework:** React 19
- **Build Tool:** Vite
- **Language:** TypeScript
- **Styling:** Custom CSS with Dark/Light/AMOLED themes
- **AI Integration:** Anthropic Claude API

### Android Native App
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Local Database:** Room (SQLite)
- **Architecture:** MVVM (Model-View-ViewModel) with Coroutines & StateFlow
- **Background Tasks:** AlarmManager for local notifications
- **Report Generation:** Native Android `PdfDocument`
- **AI Integration:** Anthropic Claude API

##  Getting Started

### React Web App
The web app is built with React, Vite, and TypeScript.
```powershell
# Install dependencies
npm install

# Run the development server
npm run dev

# Build for production
npm run build
```

### Android App
The Android app is built using native Kotlin and Jetpack Compose.
- Open the `android/` directory in Android Studio.
- Sync the Gradle project.
- Run on an emulator or a physical device.

##  Documentation
- For a detailed overview of the app's architecture, data flow, and file structures, please refer to the [workflow.md](./workflow.md) file.
