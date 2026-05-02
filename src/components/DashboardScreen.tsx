import { useState } from "react";
import type { AppState, ForecastState } from "../types";
import { formatLitres, impactDays } from "../services/calculations";
import { BottomNav, Button, Card, Icon } from "./UI";

type ForecastDay = {
  date: string;
  mm: number;
};

function localDate(date = new Date()) {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

function greetingFor(date: Date) {
  const hour = date.getHours();
  if (hour < 12) return "Good morning";
  if (hour <= 17) return "Good afternoon";
  return "Good evening";
}

function seasonFor(monthIndex: number) {
  if (monthIndex >= 2 && monthIndex <= 4) return { label: "Pre-Monsoon", color: "#b7791f", bg: "#fff7e6" };
  if (monthIndex >= 5 && monthIndex <= 8) return { label: "SW Monsoon", color: "#1565c0", bg: "#e7f0fb" };
  if (monthIndex >= 9 && monthIndex <= 11) return { label: "NE Monsoon", color: "#00796b", bg: "#e0f2f1" };
  return { label: "Dry Season", color: "#667085", bg: "#eef2f6" };
}

function monthName(monthKey: string) {
  return new Date(`${monthKey}-01T00:00:00`).toLocaleDateString("en-IN", { month: "long" });
}

function calculateStreak(dates: Set<string>, today: string) {
  let streak = 0;
  const cursor = new Date(`${today}T00:00:00`);
  while (dates.has(localDate(cursor))) {
    streak += 1;
    cursor.setDate(cursor.getDate() - 1);
  }
  return streak;
}

function RainIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M7 18a5 5 0 0 1 1-9.9A7 7 0 0 1 21 11a4 4 0 0 1-1 7" />
      <path d="M8 20v1M12 19v2M16 20v1" />
    </svg>
  );
}

function TrophyIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4Z" />
      <path d="M5 5H3v2a4 4 0 0 0 4 4M19 5h2v2a4 4 0 0 1-4 4" />
    </svg>
  );
}

function FlameIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M12 22a7 7 0 0 0 7-7c0-4-3-6.5-5-9-.5 2-1.8 3.4-3.4 4.7C9 8.8 8.7 6.7 9 4c-3 2.5-5 6-5 10.5A8 8 0 0 0 12 22Z" />
    </svg>
  );
}

export function DashboardScreen({ state, forecast, setForecast, onLogRainfall, onTab }: { state: AppState; forecast: ForecastState; setForecast: (forecast: ForecastState) => void; onLogRainfall: () => void; onTab: (screen: any) => void }) {
  const [location, setLocation] = useState(forecast.location);
  const [forecastExpanded, setForecastExpanded] = useState(Boolean(forecast.location));
  const [dismissedMilestone, setDismissedMilestone] = useState<number | null>(null);
  const now = new Date();
  const today = localDate(now);
  const season = seasonFor(now.getMonth());
  const todaysEntries = state.entries.filter((entry) => entry.date === today);
  const todayLitres = todaysEntries.reduce((sum, entry) => sum + entry.waterLitres, 0);
  const totalLitres = state.entries.reduce((sum, entry) => sum + entry.waterLitres, 0);
  const monthKey = today.slice(0, 7);
  const monthLitres = state.entries.filter((entry) => entry.date.slice(0, 7) === monthKey).reduce((sum, entry) => sum + entry.waterLitres, 0);
  const latestEntry = [...state.entries].sort((a, b) => b.date.localeCompare(a.date))[0];
  const loggedDates = new Set(state.entries.map((entry) => entry.date));
  const streak = calculateStreak(loggedDates, today);
  const tankPercent = state.tankCapacity ? Math.min(100, (todayLitres / state.tankCapacity) * 100) : 0;
  const litresToFill = Math.max(0, state.tankCapacity - todayLitres);
  const bestDay = Math.max(0, ...state.entries.map((entry) => entry.waterLitres));
  const distinctMonths = new Set(state.entries.map((entry) => entry.date.slice(0, 7))).size;
  const avgMonthlyTotal = distinctMonths ? totalLitres / distinctMonths : 0;
  const dryDays = state.entries.filter((entry) => entry.rainfallMm === 0).length;
  const monthlyProgress = avgMonthlyTotal ? Math.min(100, (monthLitres / avgMonthlyTotal) * 100) : 0;
  const milestone = [10000, 5000, 1000, 500].find((value) => totalLitres >= value);
  const showMilestone = Boolean(milestone && dismissedMilestone !== milestone);
  const forecastDays: ForecastDay[] = forecast.status === "success"
    ? [0, 3, forecast.rainfallMm, 8, 0, 12, 4].map((mm, index) => {
      const date = new Date(now);
      date.setDate(now.getDate() + index);
      return { date: localDate(date), mm };
    })
    : [];
  const nextRain = forecastDays.find((day) => day.date > today && day.mm > 5);

  function checkForecast(nextLocation = location) {
    setLocation(nextLocation);
    setForecast({ status: "loading", location: nextLocation });
    window.setTimeout(() => {
      if (!nextLocation.trim()) {
        setForecast({ status: "error", location: nextLocation, message: "Location not found" });
        return;
      }
      setForecast({ status: "success", location: nextLocation, rainfallMm: 18, message: `Light rain expected near ${nextLocation}. Estimated 18 mm today.` });
    }, 800);
  }

  function useCurrentLocation() {
    checkForecast(location.trim() || "Current location");
  }

  return (
    <>
      <header className="header">
        <div style={{ width: "100%" }}>
          <div className="header-subtitle">{greetingFor(now)}</div>
          <div className="header-title">Jal-Sanchay Tracker</div>
          <div className="row" style={{ marginTop: 6 }}>
            <span className="header-subtitle">{now.toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" })}</span>
            <span className="subtle-pill" style={{ color: season.color, background: season.bg }}>{season.label}</span>
          </div>
        </div>
      </header>
      <main className="content stack">
        {showMilestone && milestone && (
          <Card>
            <div className="row between">
              <div className="row">
                <TrophyIcon />
                <div>
                  <div className="title">Milestone reached</div>
                  <p className="supporting">You have saved {formatLitres(totalLitres)} — {impactDays(totalLitres).toFixed(1)} days of water supply.</p>
                </div>
              </div>
              <button className="icon-btn" onClick={() => setDismissedMilestone(milestone)} aria-label="Dismiss milestone">×</button>
            </div>
          </Card>
        )}
        {!todaysEntries.length && (
          <Card>
            <div className="title">Today's harvest</div>
            <p className="supporting">Did it rain today?</p>
            <Button variant="secondary" onClick={onLogRainfall}>Log Rainfall →</Button>
          </Card>
        )}
        <Card>
          <div className="row" style={{ alignItems: "center" }}>
            <div className="tank-panel">
              <div className="tank" aria-label={`${Math.round(tankPercent)}% tank fill`}>
                <div className="tank-fill" style={{ height: `${tankPercent}%` }} />
              </div>
              <div className="supporting">{formatLitres(litresToFill)} to fill · Last logged: {latestEntry?.date ?? "None"}</div>
            </div>
            <div style={{ flex: 1 }}>
              <div className="key-metric">{impactDays(totalLitres).toFixed(1)} days</div>
              <div className="supporting">days of water supply saved</div>
              <div className="title" style={{ marginTop: 16 }}>{formatLitres(totalLitres)}</div>
              <div className="supporting">collected all-time</div>
            </div>
          </div>
        </Card>
        {!state.entries.length && <Card><div className="title">Start tracking to see your savings</div></Card>}
        <div className="grid-2">
          <Card>
            <div className="supporting">This Month</div>
            <div className="title">{formatLitres(monthLitres)}</div>
          </Card>
          <Card>
            <div className="supporting">Streak</div>
            <div className="title row">{streak > 0 ? <><FlameIcon /> {streak} day streak</> : "Start your streak today"}</div>
          </Card>
        </div>
        <Card>
          <div className="row between" style={{ gap: 0 }}>
            <div style={{ flex: 1 }}>
              <div className="supporting">Best Day</div>
              <div className="title">{formatLitres(bestDay)}</div>
            </div>
            <div style={{ width: 1, alignSelf: "stretch", background: "var(--border)" }} />
            <div style={{ flex: 1, paddingLeft: 12 }}>
              <div className="supporting">Avg/Month</div>
              <div className="title">{formatLitres(avgMonthlyTotal)}</div>
            </div>
            <div style={{ width: 1, alignSelf: "stretch", background: "var(--border)" }} />
            <div style={{ flex: 1, paddingLeft: 12 }}>
              <div className="supporting">Dry Days</div>
              <div className="title">{dryDays}</div>
            </div>
          </div>
        </Card>
        {distinctMonths >= 2 && (
          <Card>
            <div className="supporting">{monthName(monthKey)} — {Math.round(monthlyProgress)}% of monthly average</div>
            <div className="progress" style={{ marginTop: 10 }}><span style={{ width: `${monthlyProgress}%` }} /></div>
          </Card>
        )}
        {nextRain && forecast.status === "success" && (
          <section className="card" style={{ background: "var(--primary-soft)", borderColor: "var(--primary)" }}>
            <div className="row">
              <RainIcon />
              <div>
                <div className="title">Rain expected {new Date(`${nextRain.date}T00:00:00`).toLocaleDateString("en-IN", { weekday: "short" })} · {nextRain.mm}mm</div>
                <div className="supporting">Prepare your tank</div>
              </div>
            </div>
          </section>
        )}
        {!forecastExpanded && !location.trim() ? (
          <button className="card-button" onClick={() => setForecastExpanded(true)}>
            <div className="row between">
              <strong>Set your location for rain forecast</strong>
              <span>→</span>
            </div>
          </button>
        ) : (
          <Card>
            <div className="row between">
              <div>
                <div className="title">Forecast</div>
                <div className="supporting">Plan harvesting before rain arrives.</div>
              </div>
              <Icon name="location" />
            </div>
            <div style={{ height: 12 }} />
            <label className="label">Location</label>
            <input className="input" value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Enter your location" />
            <div style={{ height: 12 }} />
            <div className="forecast-actions">
              <Button full onClick={() => checkForecast()}>{forecast.status === "loading" ? "Checking..." : "Check forecast"}</Button>
              <Button variant="secondary" full onClick={useCurrentLocation}>Use current location</Button>
            </div>
            {forecast.status === "error" && <div className="error">{forecast.message}</div>}
            {forecast.status === "success" && (
              <div className="grid-2" style={{ marginTop: 12 }}>
                {forecastDays.map((day) => (
                  <div key={day.date} className="subtle-pill" style={{ justifyContent: "space-between" }}>
                    <span>{new Date(`${day.date}T00:00:00`).toLocaleDateString("en-IN", { weekday: "short" })}</span>
                    <strong>{day.mm}mm</strong>
                  </div>
                ))}
              </div>
            )}
          </Card>
        )}
      </main>
      <footer className="actions">
        <Button full onClick={onLogRainfall}><span className="row" style={{ justifyContent: "center" }}><Icon name="plus" /> Log Rainfall</span></Button>
      </footer>
      <BottomNav active="dashboard" onChange={onTab} />
    </>
  );
}
