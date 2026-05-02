import { useEffect, useReducer, useState } from "react";
import { DashboardScreen } from "./components/DashboardScreen";
import { HistoryScreen } from "./components/HistoryScreen";
import { Onboarding } from "./components/Onboarding";
import { RainfallDetailScreen } from "./components/RainfallDetailScreen";
import { RainfallEntryScreen } from "./components/RainfallEntryScreen";
import { ReportsScreen } from "./components/ReportsScreen";
import { SettingsScreen } from "./components/SettingsScreen";
import { SplashScreen } from "./components/SplashScreen";
import { TipsScreen } from "./components/TipsScreen";
import { ConfirmDialog, Toast } from "./components/UI";
import { initialAppState } from "./mockData";
import { recalculateEntries } from "./services/calculations";
import type { AppState, Entry, ForecastState, MainTab, Screen } from "./types";

type Action =
  | { type: "patch"; payload: Partial<AppState> }
  | { type: "saveEntry"; payload: Entry }
  | { type: "deleteEntry"; payload: string }
  | { type: "reset" }
  | { type: "import"; payload: AppState };

function reducer(state: AppState, action: Action): AppState {
  switch (action.type) {
    case "patch": {
      const next = { ...state, ...action.payload };
      const shouldRecalculate = ["roofArea", "unit", "tankCapacity", "runoffCoeff"].some((key) => key in action.payload);
      return shouldRecalculate ? { ...next, entries: recalculateEntries(next.entries, next.roofArea, next.unit, next.runoffCoeff, next.tankCapacity) } : next;
    }
    case "saveEntry": {
      const exists = state.entries.some((entry) => entry.id === action.payload.id);
      return { ...state, entries: exists ? state.entries.map((entry) => (entry.id === action.payload.id ? action.payload : entry)) : [...state.entries, action.payload] };
    }
    case "deleteEntry":
      return { ...state, entries: state.entries.filter((entry) => entry.id !== action.payload) };
    case "reset":
      return { ...initialAppState, entries: [], setupDone: false };
    case "import":
      return action.payload;
    default:
      return state;
  }
}

export default function App() {
  const [state, dispatch] = useReducer(reducer, initialAppState);
  const [screen, setScreen] = useState<Screen>("splash");
  const [editingEntry, setEditingEntry] = useState<Entry | undefined>();
  const [detailEntry, setDetailEntry] = useState<Entry | undefined>();
  const [deleteEntry, setDeleteEntry] = useState<Entry | undefined>();
  const [toast, setToast] = useState("");
  const [historyScroll, setHistoryScroll] = useState(0);
  const [forecast, setForecast] = useState<ForecastState>({ status: "idle", location: "" });

  const themeName = state.theme === "dark" && state.amoled ? "amoled" : state.theme;

  useEffect(() => {
    if (screen !== "splash") return;
    const timer = window.setTimeout(() => setScreen(state.setupDone ? "dashboard" : "onboarding"), 2000);
    return () => window.clearTimeout(timer);
  }, [screen, state.setupDone]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(""), 2200);
    return () => window.clearTimeout(timer);
  }, [toast]);

  function goTab(tab: MainTab) {
    setEditingEntry(undefined);
    setDetailEntry(undefined);
    setScreen(tab);
  }

  function exportJson() {
    const blob = new Blob([JSON.stringify(state, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "jal-sanchay-data.json";
    a.click();
    URL.revokeObjectURL(url);
    setToast("Changes saved successfully");
  }

  function exportCsv() {
    const rows = ["Date,Rainfall mm,Water Saved L", ...state.entries.map((entry) => `${entry.date},${entry.rainfallMm},${entry.waterLitres.toFixed(1)}`)];
    const blob = new Blob([rows.join("\n")], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "jal-sanchay-history.csv";
    a.click();
    URL.revokeObjectURL(url);
    setToast("Export ready");
  }

  function importJson(file: File) {
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const parsed = JSON.parse(String(reader.result)) as AppState;
        if (!Array.isArray(parsed.entries)) throw new Error("Invalid file");
        dispatch({ type: "import", payload: parsed });
        setToast("Changes saved successfully");
      } catch {
        setToast("Import failed");
      }
    };
    reader.readAsText(file);
  }

  function currentScreen() {
    if (screen === "splash") return <SplashScreen />;
    if (screen === "onboarding") return <Onboarding state={state} onFinish={(updates) => { dispatch({ type: "patch", payload: updates }); setToast("Changes saved successfully"); setScreen("dashboard"); }} />;
    if (screen === "dashboard") return <DashboardScreen state={state} forecast={forecast} setForecast={setForecast} onLogRainfall={() => { setEditingEntry(undefined); setScreen("rainfallEntry"); }} onTab={goTab} />;
    if (screen === "reports") return <ReportsScreen state={state} onTab={goTab} onExport={exportCsv} />;
    if (screen === "history") {
      return (
        <HistoryScreen
          state={state}
          scrollTop={historyScroll}
          onScrollChange={setHistoryScroll}
          onTab={goTab}
          onEdit={(entry) => { setEditingEntry(entry); setScreen("rainfallEntry"); }}
          onDelete={setDeleteEntry}
        />
      );
    }
    if (screen === "rainfallDetail" && detailEntry) {
      const latest = state.entries.find((entry) => entry.id === detailEntry.id) ?? detailEntry;
      return <RainfallDetailScreen entry={latest} onBack={() => setScreen("history")} onEdit={() => { setEditingEntry(latest); setScreen("rainfallEntry"); }} onDelete={() => setDeleteEntry(latest)} />;
    }
    if (screen === "rainfallEntry") {
      const returnScreen = editingEntry ? "history" : "dashboard";
      return (
        <RainfallEntryScreen
          state={state}
          editingEntry={editingEntry}
          returnTo={returnScreen}
          onBack={() => setScreen(returnScreen)}
          onSave={(entry) => {
            dispatch({ type: "saveEntry", payload: entry });
            setToast(editingEntry ? "Changes saved successfully" : "Entry saved!");
            setEditingEntry(undefined);
            setScreen(returnScreen);
          }}
        />
      );
    }
    if (screen === "tips") return <TipsScreen state={state} forecast={forecast} onTab={goTab} />;
    if (screen === "settings") {
      return (
        <SettingsScreen
          state={state}
          entries={state.entries}
          onTab={goTab}
          onSave={(updates) => {
            dispatch({ type: "patch", payload: updates });
            setToast("Settings updated. Data recalculated.");
            setScreen("dashboard");
          }}
          onReset={() => setDeleteEntry({ id: "__reset__", date: "", rainfallMm: 0, waterLitres: 0 })}
          onExport={exportJson}
          onImport={importJson}
          onToast={setToast}
        />
      );
    }
    return <DashboardScreen state={state} forecast={forecast} setForecast={setForecast} onLogRainfall={() => setScreen("rainfallEntry")} onTab={goTab} />;
  }

  return (
    <div className="app-shell" data-theme={themeName}>
      <div className="phone">{currentScreen()}</div>
      <Toast message={toast} />
      {deleteEntry && deleteEntry.id !== "__reset__" && (
        <ConfirmDialog
          title="Delete this entry?"
          message="This record will be removed from history."
          confirmLabel="Yes"
          onCancel={() => setDeleteEntry(undefined)}
          onConfirm={() => {
            dispatch({ type: "deleteEntry", payload: deleteEntry.id });
            setDeleteEntry(undefined);
            setToast("Changes saved successfully");
            setScreen("history");
          }}
        />
      )}
      {deleteEntry?.id === "__reset__" && (
        <ConfirmDialog
          title="This will delete all data. Continue?"
          message="Setup values and rainfall history will be cleared."
          confirmLabel="Reset"
          onCancel={() => setDeleteEntry(undefined)}
          onConfirm={() => {
            dispatch({ type: "reset" });
            setDeleteEntry(undefined);
            setToast("Changes saved successfully");
            setScreen("onboarding");
          }}
        />
      )}
    </div>
  );
}
