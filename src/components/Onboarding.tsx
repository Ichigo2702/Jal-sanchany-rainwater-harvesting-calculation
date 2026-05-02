import { useState } from "react";
import type { AppState, Unit } from "../types";
import { runoffOptions } from "../services/calculations";
import { Button, Card, Header, Segmented } from "./UI";

export function Onboarding({ state, onFinish }: { state: AppState; onFinish: (updates: Partial<AppState>) => void }) {
  const [step, setStep] = useState(0);
  const [roofArea, setRoofArea] = useState(state.roofArea);
  const [unit, setUnit] = useState<Unit>(state.unit);
  const [tankCapacity, setTankCapacity] = useState(state.tankCapacity);
  const [runoffCoeff, setRunoffCoeff] = useState(state.runoffCoeff);
  const progress = ((step + 1) / 3) * 100;

  return (
    <>
      <Header title="Setup" subtitle={`Step ${step + 1} of 3`} />
      <main className="content stack">
        <div className="progress"><span style={{ width: `${progress}%` }} /></div>
        {step === 0 && (
          <Card>
            <label className="label">Roof Area</label>
            <input className="input" type="number" value={roofArea} onChange={(e) => setRoofArea(Number(e.target.value))} />
            <div style={{ height: 12 }} />
            <Segmented value={unit} onChange={setUnit} options={[{ label: "sq ft", value: "sqft" }, { label: "sq m", value: "sqm" }]} />
            <p className="supporting">Typical 3-BHK terrace is about 800-1200 sq ft.</p>
          </Card>
        )}
        {step === 1 && (
          <Card>
            <label className="label">Tank Capacity</label>
            <input className="input" type="number" value={tankCapacity} onChange={(e) => setTankCapacity(Number(e.target.value))} />
            <p className="supporting">Enter capacity in litres.</p>
          </Card>
        )}
        {step === 2 && (
          <div className="stack">
            <Card>
              <div className="title">Runoff Coefficient</div>
              <p className="supporting">Runoff coefficient = % of rainwater collected from roof.</p>
            </Card>
            <div className="grid-2">
              {runoffOptions.map((option) => (
                <button key={option.label} className={`card-button ${runoffCoeff === option.coeff ? "selected" : ""}`} onClick={() => setRunoffCoeff(option.coeff)}>
                  <strong>{option.label}</strong>
                  <div className="supporting">{option.coeff}</div>
                </button>
              ))}
            </div>
          </div>
        )}
      </main>
      <footer className="actions row">
        <Button variant="secondary" full onClick={() => (step === 0 ? undefined : setStep(step - 1))}>Back</Button>
        {step < 2 ? (
          <Button full onClick={() => setStep(step + 1)}>Next</Button>
        ) : (
          <Button full onClick={() => onFinish({ setupDone: true, roofArea, unit, tankCapacity, runoffCoeff })}>Start Tracking</Button>
        )}
      </footer>
    </>
  );
}
