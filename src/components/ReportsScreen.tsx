import { useMemo, useState } from "react";
import { getSeasonAnalysis } from "../services/aiService";
import { formatLitres, impactDays, runoffOptions } from "../services/calculations";
import { BottomNav, Button, Card, Header } from "./UI";
import type { AppState, Entry, MainTab } from "../types";

type MonthlyRow = {
  monthKey: string;
  label: string;
  rainfall: number;
  water: number;
};

function buildMonthlyRows(entries: Entry[]) {
  const rows = new Map<string, MonthlyRow>();
  entries.forEach((entry) => {
    const monthKey = entry.date.slice(0, 7);
    const current = rows.get(monthKey) ?? {
      monthKey,
      label: new Date(`${monthKey}-01T00:00:00`).toLocaleDateString("en-IN", { month: "long", year: "numeric" }),
      rainfall: 0,
      water: 0
    };
    rows.set(monthKey, {
      ...current,
      rainfall: current.rainfall + entry.rainfallMm,
      water: current.water + entry.waterLitres
    });
  });
  return [...rows.values()].sort((a, b) => a.monthKey.localeCompare(b.monthKey));
}

export function ReportsScreen({ state, onTab, onExport }: { state: AppState; onTab: (tab: MainTab) => void; onExport: () => void }) {
  const currentYear = String(new Date().getFullYear());
  const years = useMemo(() => {
    const entryYears = [...new Set(state.entries.map((entry) => entry.date.slice(0, 4)))].sort((a, b) => b.localeCompare(a));
    return entryYears.length ? entryYears : [currentYear];
  }, [state.entries, currentYear]);
  const [selectedYear, setSelectedYear] = useState(years.includes(currentYear) ? currentYear : years[0]);
  const [aiOpen, setAiOpen] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResponse, setAiResponse] = useState("");
  const allRows = useMemo(() => buildMonthlyRows(state.entries), [state.entries]);
  const rows = allRows.filter((row) => row.monthKey.startsWith(selectedYear));
  const totalRainfall = state.entries.reduce((sum, entry) => sum + entry.rainfallMm, 0);
  const totalWater = state.entries.reduce((sum, entry) => sum + entry.waterLitres, 0);
  const filteredRainfall = rows.reduce((sum, row) => sum + row.rainfall, 0);
  const filteredWater = rows.reduce((sum, row) => sum + row.water, 0);
  const max = Math.max(1, ...rows.map((item) => item.water));
  const bestMonth = rows.reduce<MonthlyRow | undefined>((best, row) => !best || row.water > best.water ? row : best, undefined);
  const latestMonth = rows[rows.length - 1];
  const previousMonth = rows[rows.length - 2];
  const deltaDays = latestMonth && previousMonth ? impactDays(latestMonth.water) - impactDays(previousMonth.water) : 0;
  const runoffLabel = runoffOptions.find((option) => option.coeff === state.runoffCoeff)?.label ?? "Selected";

  async function loadAiInsight() {
    setAiOpen(true);
    setAiLoading(true);
    setAiResponse("");
    const response = await getSeasonAnalysis(
      { roofArea: state.roofArea, unit: state.unit, runoffCoeff: state.runoffCoeff, runoffLabel, tankCapacity: state.tankCapacity },
      rows,
      totalWater,
      Number(impactDays(totalWater).toFixed(1))
    );
    setAiResponse(response);
    setAiLoading(false);
  }

  return (
    <>
      <Header title="Monthly Reports" />
      <main className="content stack">
        <div className="row between">
          <Button variant="secondary" onClick={() => setSelectedYear(years[Math.min(years.length - 1, years.indexOf(selectedYear) + 1)] ?? selectedYear)}>←</Button>
          <div className="title">{selectedYear}</div>
          <Button variant="secondary" onClick={() => setSelectedYear(years[Math.max(0, years.indexOf(selectedYear) - 1)] ?? selectedYear)}>→</Button>
        </div>
        <Card>
          <div className="title">All-Time Summary</div>
          <div className="grid-2" style={{ marginTop: 12 }}>
            <div><div className="supporting">Rainfall</div><strong>{totalRainfall.toFixed(1)} mm</strong></div>
            <div><div className="supporting">Saved</div><strong>{formatLitres(totalWater)}</strong></div>
            <div><div className="supporting">Impact</div><strong>{impactDays(totalWater).toFixed(1)} days</strong></div>
            <div><div className="supporting">Months</div><strong>{allRows.length}</strong></div>
          </div>
        </Card>
        <Card>
          <table className="table">
            <thead>
              <tr><th>Month</th><th>Rainfall</th><th>Saved</th><th>Days</th></tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr key={item.monthKey}>
                  <td>{item.label}</td>
                  <td>{item.rainfall.toFixed(1)} mm</td>
                  <td>{Math.round(item.water)} L</td>
                  <td>{impactDays(item.water).toFixed(1)}</td>
                </tr>
              ))}
              <tr style={{ fontWeight: 800, background: "var(--surface-strong)" }}>
                <td>Total</td>
                <td>{filteredRainfall.toFixed(1)} mm</td>
                <td>{Math.round(filteredWater)} L</td>
                <td>{impactDays(filteredWater).toFixed(1)}</td>
              </tr>
            </tbody>
          </table>
        </Card>
        <Card>
          <div className="title">Monthly Trend</div>
          {rows.length < 2 ? (
            <p className="supporting">Not enough data to display trends</p>
          ) : (
            <div className="bar-wrap">
              {rows.map((item) => (
                <div key={item.monthKey} style={{ flex: 1, textAlign: "center" }}>
                  <div className="bar" style={{ height: `${(item.water / max) * 100}%` }} />
                  <div className="supporting">{item.monthKey.slice(5)}</div>
                </div>
              ))}
            </div>
          )}
        </Card>
        {bestMonth && (
          <Card>
            <div className="title">Best month: {bestMonth.label}</div>
            <p className="supporting">{formatLitres(bestMonth.water)} · {impactDays(bestMonth.water).toFixed(1)} days</p>
          </Card>
        )}
        {latestMonth && previousMonth && (
          <Card>
            <div className="title">Month-over-month insight</div>
            <p className="supporting">
              {latestMonth.label} saved {impactDays(latestMonth.water).toFixed(1)} days — <span style={{ color: deltaDays >= 0 ? "var(--accent)" : "var(--danger)" }}>{deltaDays >= 0 ? "↑" : "↓"} {Math.abs(deltaDays).toFixed(1)} days</span> vs {previousMonth.label}
            </p>
          </Card>
        )}
        <Card>
          <div className="row between">
            <div>
              <div className="title">AI Season Analysis</div>
              <p className="supporting">Generate concise observations from this year's monthly data.</p>
            </div>
            <Button variant="secondary" onClick={aiResponse ? loadAiInsight : () => setAiOpen(!aiOpen)}>{aiResponse ? "Regenerate" : aiOpen ? "Hide" : "Open"}</Button>
          </div>
          {aiOpen && (
            <div style={{ marginTop: 12 }}>
              {!aiResponse && <Button full onClick={loadAiInsight}>{aiLoading ? "Generating..." : "Get AI Insight"}</Button>}
              {aiLoading && <p className="supporting">Loading insight...</p>}
              {aiResponse && <p className="supporting" style={{ whiteSpace: "pre-wrap" }}>{aiResponse}</p>}
            </div>
          )}
        </Card>
      </main>
      <footer className="actions">
        <Button full onClick={onExport}>Export CSV</Button>
      </footer>
      <BottomNav active="reports" onChange={onTab} />
    </>
  );
}
