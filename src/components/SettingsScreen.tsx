import { useRef, useState } from "react";
import type { AppState, Entry, MainTab, Unit } from "../types";
import { runoffOptions } from "../services/calculations";
import { BottomNav, Button, Card, Header, Icon, Segmented } from "./UI";

export function SettingsScreen({ state, entries, onTab, onSave, onReset, onExport, onImport, onToast }: { state: AppState; entries: Entry[]; onTab: (tab: MainTab) => void; onSave: (updates: Partial<AppState>) => void; onReset: () => void; onExport: () => void; onImport: (file: File) => void; onToast: (message: string) => void }) {
  const [roofArea, setRoofArea] = useState(state.roofArea);
  const [unit, setUnit] = useState<Unit>(state.unit);
  const [tankCapacity, setTankCapacity] = useState(state.tankCapacity);
  const [runoffCoeff, setRunoffCoeff] = useState(state.runoffCoeff);
  const [theme, setTheme] = useState(state.theme);
  const [amoled, setAmoled] = useState(state.amoled);
  const [saved, setSaved] = useState(false);
  const fileRef = useRef<HTMLInputElement | null>(null);
  const dbKb = Math.round(entries.length * 0.15 * 10) / 10;

  function saveChanges() {
    setSaved(true);
    window.setTimeout(() => {
      setSaved(false);
      onSave({ roofArea, unit, tankCapacity, runoffCoeff, theme, amoled });
    }, 1500);
  }

  function showComingSoon() {
    onToast("Coming in v1.1");
  }

  return (
    <>
      <Header title="Settings" />
      <main className="content stack">
        <Card>
          <label className="label">Roof Area</label>
          <input className="input" type="number" value={roofArea} onChange={(event) => setRoofArea(Number(event.target.value))} />
          <div style={{ height: 12 }} />
          <Segmented value={unit} onChange={setUnit} options={[{ label: "sq ft", value: "sqft" }, { label: "sq m", value: "sqm" }]} />
        </Card>
        <Card>
          <label className="label">Tank Capacity (L)</label>
          <input className="input" type="number" value={tankCapacity} onChange={(event) => setTankCapacity(Number(event.target.value))} />
        </Card>
        <Card>
          <div className="title">Runoff Coefficient</div>
          <p className="supporting">Runoff coefficient = % of rainwater collected from roof.</p>
          <div className="grid-2">
            {runoffOptions.map((option) => (
              <button key={option.label} className={`card-button ${runoffCoeff === option.coeff ? "selected" : ""}`} onClick={() => setRunoffCoeff(option.coeff)}>
                <strong>{option.label}</strong>
                <div className="supporting">{option.coeff}</div>
              </button>
            ))}
          </div>
        </Card>
        <Card>
          <div className="title">Appearance</div>
          <div style={{ height: 12 }} />
          <Segmented value={theme} onChange={setTheme} options={[{ label: "Light", value: "light" }, { label: "Dark", value: "dark" }]} />
          {theme === "dark" && (
            <label className="row between" style={{ marginTop: 12 }}>
              <span className="row"><Icon name="moon" /> AMOLED true black</span>
              <input type="checkbox" checked={amoled} onChange={(event) => setAmoled(event.target.checked)} />
            </label>
          )}
        </Card>
        <Card>
          <div className="title">Data</div>
          <p className="supporting">Export or import app data as JSON.</p>
          <div className="row">
            <Button variant="secondary" full onClick={onExport}><span className="row" style={{ justifyContent: "center" }}><Icon name="download" /> Export</span></Button>
            <Button variant="secondary" full onClick={() => fileRef.current?.click()}><span className="row" style={{ justifyContent: "center" }}><Icon name="upload" /> Import</span></Button>
          </div>
          <input ref={fileRef} hidden type="file" accept="application/json" onChange={(event) => event.target.files?.[0] && onImport(event.target.files[0])} />
        </Card>
        <Card>
          <div className="title">Notifications</div>
          <p className="supporting">Coming in v1.1</p>
          <button className="card-button" style={{ width: "100%", opacity: 0.65, boxShadow: "none" }} onClick={showComingSoon}>
            <div className="row between"><span>Rain day reminders</span><input type="checkbox" disabled /></div>
          </button>
          <div style={{ height: 8 }} />
          <button className="card-button" style={{ width: "100%", opacity: 0.65, boxShadow: "none" }} onClick={showComingSoon}>
            <div className="row between"><span>Milestone alerts</span><input type="checkbox" disabled /></div>
          </button>
        </Card>
        <Card>
          <div className="title">About</div>
          <p className="supporting">App version v1.0<br />CPHEEO formula reference: 135 L = 1 day of household water supply.<br />Database: {entries.length} entries · {dbKb}KB</p>
        </Card>
        <p className="supporting">Changing setup values will recalculate all historical data.</p>
      </main>
      <footer className="actions stack">
        <button className="btn primary full" style={saved ? { background: "var(--accent)" } : undefined} onClick={saveChanges}>{saved ? "✓ Saved!" : "Save Changes"}</button>
        <Button variant="danger" full onClick={onReset}>Reset App</Button>
      </footer>
      <BottomNav active="settings" onChange={onTab} />
    </>
  );
}
