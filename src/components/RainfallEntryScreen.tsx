import { useMemo, useState } from "react";
import type { AppState, Entry } from "../types";
import { calculateWaterLitres, formatLitres } from "../services/calculations";
import { Button, Card, Header, Icon } from "./UI";

export function RainfallEntryScreen({ state, editingEntry, returnTo, onBack, onSave }: { state: AppState; editingEntry?: Entry; returnTo: string; onBack: () => void; onSave: (entry: Entry) => void }) {
  const today = new Date();
  const minDate = new Date(today);
  minDate.setDate(today.getDate() - 7);
  const [date, setDate] = useState(editingEntry?.date ?? today.toISOString().slice(0, 10));
  const [rainfall, setRainfall] = useState(editingEntry ? String(editingEntry.rainfallMm) : "");
  const rainfallNumber = Number(rainfall);
  const error = !rainfall.trim() ? "Rainfall is required" : Number.isNaN(rainfallNumber) || rainfallNumber < 0 ? "Enter a valid rainfall amount" : "";
  const estimated = useMemo(() => (error ? 0 : calculateWaterLitres(state.roofArea, state.unit, rainfallNumber, state.runoffCoeff, state.tankCapacity)), [error, rainfallNumber, state]);

  function save() {
    if (error) return;
    onSave({
      id: editingEntry?.id ?? `${date}-${Date.now()}`,
      date,
      rainfallMm: rainfallNumber,
      waterLitres: estimated
    });
  }

  return (
    <>
      <Header title={editingEntry ? "Edit Entry" : "Log Rainfall"} subtitle={returnTo === "history" ? "Returns to History" : undefined} onBack={onBack} />
      <main className="content stack">
        <Card>
          <label className="label">Date</label>
          <input className="input" type="date" value={date} min={minDate.toISOString().slice(0, 10)} max={today.toISOString().slice(0, 10)} onChange={(e) => setDate(e.target.value)} />
        </Card>
        <Card>
          <label className="label">Rainfall in mm</label>
          <input className="input" type="number" value={rainfall} onChange={(e) => setRainfall(e.target.value)} placeholder="e.g. 25" />
          {error && <div className="error">{error}</div>}
          <p className="supporting"><strong>Estimated collection: {formatLitres(estimated)}</strong></p>
          <div className="subtle-pill"><Icon name="info" /> Based on roof area and runoff coefficient</div>
        </Card>
      </main>
      <footer className="actions row">
        <Button variant="secondary" full onClick={onBack}>Cancel</Button>
        <Button full onClick={save} disabled={Boolean(error)}>{editingEntry ? "Save Changes" : "Save Entry"}</Button>
      </footer>
    </>
  );
}
