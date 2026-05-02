import { useEffect, useRef, useState } from "react";
import type { AppState, Entry, MainTab } from "../types";
import { areaToSqFt, formatLitres, runoffOptions } from "../services/calculations";
import { BottomNav, Card, Header, Icon } from "./UI";

function monthLabel(monthKey: string) {
  return new Date(`${monthKey}-01T00:00:00`).toLocaleDateString("en-IN", { month: "long", year: "numeric" });
}

export function HistoryScreen({ state, scrollTop, onScrollChange, onTab, onEdit, onDelete }: { state: AppState; scrollTop: number; onScrollChange: (value: number) => void; onTab: (tab: MainTab) => void; onEdit: (entry: Entry) => void; onDelete: (entry: Entry) => void }) {
  const ref = useRef<HTMLElement | null>(null);
  const startX = useRef(0);
  const [hintId, setHintId] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState("all");
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const ordered = [...state.entries].sort((a, b) => b.date.localeCompare(a.date));
  const monthOptions = [...new Set(ordered.map((entry) => entry.date.slice(0, 7)))].sort((a, b) => b.localeCompare(a));
  const filtered = selectedMonth === "all" ? ordered : ordered.filter((entry) => entry.date.startsWith(selectedMonth));
  const totalRainfall = filtered.reduce((sum, entry) => sum + entry.rainfallMm, 0);
  const totalLitres = filtered.reduce((sum, entry) => sum + entry.waterLitres, 0);
  const runoffLabel = runoffOptions.find((option) => option.coeff === state.runoffCoeff)?.label ?? "Selected";

  useEffect(() => {
    if (ref.current) ref.current.scrollTop = scrollTop;
  }, []);

  return (
    <>
      <Header title="Rainfall History" />
      <main ref={ref} className="content stack history-list" onScroll={(event) => onScrollChange(event.currentTarget.scrollTop)}>
        <Card>
          <div className="supporting">{filtered.length} entries · {totalRainfall.toFixed(1)}mm total · {formatLitres(totalLitres)} collected</div>
        </Card>
        <select className="input" value={selectedMonth} onChange={(event) => setSelectedMonth(event.target.value)}>
          <option value="all">All entries</option>
          {monthOptions.map((month) => <option key={month} value={month}>{monthLabel(month)}</option>)}
        </select>
        {!filtered.length ? (
          <Card>
            <div className="title">No records yet</div>
            <p className="supporting">Add your first rainfall entry</p>
          </Card>
        ) : (
          filtered.map((entry, index) => {
            const prev = ordered[ordered.findIndex((item) => item.id === entry.id) + 1];
            const trend = prev ? (entry.rainfallMm > prev.rainfallMm ? "↑ higher than previous" : entry.rainfallMm < prev.rainfallMm ? "↓ lower than previous" : "same as previous") : "first record";
            const trendColor = trend.startsWith("↑") ? "var(--accent)" : trend.startsWith("↓") ? "var(--danger)" : "var(--muted)";
            const raw = areaToSqFt(state.roofArea, state.unit) * entry.rainfallMm * 0.0929 * state.runoffCoeff;
            const capped = raw > state.tankCapacity;
            const expanded = expandedId === entry.id;
            return (
              <button
                key={entry.id}
                className="card-button history-item"
                onPointerDown={(event) => { startX.current = event.clientX; setHintId(entry.id); }}
                onPointerUp={(event) => {
                  const delta = event.clientX - startX.current;
                  if (delta > 55) onEdit(entry);
                  else if (delta < -55) onDelete(entry);
                  else setExpandedId(expanded ? null : entry.id);
                }}
              >
                <div className="row between">
                  <div className="history-meta">
                    <div className="title">{entry.rainfallMm} mm</div>
                    <div className="supporting">{formatLitres(entry.waterLitres)}</div>
                    <div className="supporting">{entry.date}</div>
                  </div>
                  <div className="subtle-pill" style={{ color: trendColor }}>{trend}</div>
                </div>
                {expanded && (
                  <div className="supporting" style={{ marginTop: 10 }}>
                    {entry.rainfallMm}mm × {state.roofArea} {state.unit} × 0.0929 × {state.runoffCoeff} ({runoffLabel}) = {formatLitres(entry.waterLitres)}
                    {capped && <><br />Capped at tank capacity ({formatLitres(state.tankCapacity)})</>}
                  </div>
                )}
                {hintId === entry.id && <div className="supporting" style={{ marginTop: 8 }}><Icon name="edit" /> Swipe right to edit. Swipe left to delete. Tap to expand.</div>}
              </button>
            );
          })
        )}
      </main>
      <BottomNav active="history" onChange={onTab} />
    </>
  );
}
