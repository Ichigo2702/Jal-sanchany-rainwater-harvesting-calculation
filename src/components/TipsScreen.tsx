import { useState } from "react";
import { askGlossary, getTips } from "../services/aiService";
import { formatLitres, runoffOptions } from "../services/calculations";
import type { AppState, ForecastState, MainTab } from "../types";
import { BottomNav, Button, Card, Header } from "./UI";

const tips = [
  ["Clean your roof before monsoon", "Remove leaves, dust, and debris so the first rain does not carry contaminants into storage."],
  ["Install a first-flush diverter", "Divert the first few minutes of rainfall away from the tank to improve stored water quality."],
  ["Check tank for cracks before season", "Inspect joints, lids, outlets, and overflow paths before heavy rain starts."],
  ["Ideal tank materials", "Ferro-cement and food-grade plastic are durable choices for household rainwater storage."],
  ["Cover your tank", "A sealed cover reduces mosquito breeding and keeps leaves or debris out."],
  ["Runoff coefficient", "Concrete and metal roofs collect more rainwater than green roofs because less water is absorbed."],
  ["Inspect gutters monthly", "Clear blocked gutters so rain reaches the tank instead of overflowing along the roof edge."],
  ["Keep overflow directed safely", "Route overflow away from foundations and into a recharge pit or garden area."],
  ["Use a mesh filter", "A simple inlet mesh blocks leaves, grit, and insects before water enters the tank."],
  ["Record zero-rain days too", "Logging 0 mm helps monthly reports distinguish dry days from missing data."]
];

const glossary = [
  ["Runoff Coefficient", "The percentage of rainfall that can be collected from a roof surface."],
  ["Catchment Area", "The roof or surface area from which rainwater is collected."],
  ["First Flush", "Initial rainwater diverted away because it may contain dust and roof contaminants."],
  ["Water Harvesting Potential", "The theoretical maximum water a catchment can collect from rainfall over a period."],
  ["CPHEEO Standard", "A planning reference where 135 litres per person per day is treated as a domestic water supply benchmark."]
];

type QaPair = {
  question: string;
  answer: string;
};

function seasonFor(monthIndex: number) {
  if (monthIndex >= 2 && monthIndex <= 4) return { label: "Pre-Monsoon", tip: "Now is the best time to clean your roof and inspect your tank before the rains arrive." };
  if (monthIndex >= 5 && monthIndex <= 8) return { label: "SW Monsoon", tip: "Log daily during peak monsoon — small amounts add up." };
  if (monthIndex >= 9 && monthIndex <= 11) return { label: "NE Monsoon", tip: "NE monsoon active. Keep tank covered, diverter clean." };
  return { label: "Dry Season", tip: "Dry season. Audit your tank before pre-monsoon." };
}

function localDate(date = new Date()) {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
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

function CalendarIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M8 2v4M16 2v4M4 10h16M5 5h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" />
    </svg>
  );
}

export function TipsScreen({ state, forecast, onTab }: { state: AppState; forecast: ForecastState; onTab: (tab: MainTab) => void }) {
  const [tab, setTab] = useState<"tips" | "glossary">("tips");
  const [open, setOpen] = useState<number | null>(0);
  const [openedTips, setOpenedTips] = useState<Set<number>>(new Set([0]));
  const [query, setQuery] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const [aiTip, setAiTip] = useState("");
  const [question, setQuestion] = useState("");
  const [qaLoading, setQaLoading] = useState(false);
  const [qaPairs, setQaPairs] = useState<QaPair[]>([]);
  const season = seasonFor(new Date().getMonth());
  const data = (tab === "tips" ? tips : glossary).filter(([title, body]) => `${title} ${body}`.toLowerCase().includes(query.toLowerCase()));
  const progress = (openedTips.size / tips.length) * 100;
  const runoffLabel = runoffOptions.find((option) => option.coeff === state.runoffCoeff)?.label ?? "Selected";
  const streak = calculateStreak(new Set(state.entries.map((entry) => entry.date)), localDate());
  const locName = forecast.location || "your area";

  function toggleTip(index: number) {
    setOpen(open === index ? null : index);
    setOpenedTips((current) => new Set(current).add(index));
  }

  async function loadTips() {
    setAiLoading(true);
    const response = await getTips(
      { roofArea: state.roofArea, unit: state.unit, runoffCoeff: state.runoffCoeff, runoffLabel, tankCapacity: state.tankCapacity },
      state.entries,
      locName,
      season.label,
      streak
    );
    setAiTip(response);
    setAiLoading(false);
  }

  async function askQuestion() {
    if (!question.trim()) return;
    setQaLoading(true);
    const answer = await askGlossary(question, { roofArea: state.roofArea, unit: state.unit, runoffCoeff: state.runoffCoeff, runoffLabel, tankCapacity: state.tankCapacity });
    setQaPairs((current) => [...current, { question, answer }]);
    setQuestion("");
    setQaLoading(false);
  }

  return (
    <>
      <Header title="Tips & Education" />
      <main className="content stack">
        <section className="card" style={{ borderColor: "var(--primary)" }}>
          <div className="row">
            <CalendarIcon />
            <div>
              <div className="title">{season.label}</div>
              <p className="supporting">{season.tip}</p>
            </div>
          </div>
        </section>
        <Card>
          <div className="supporting">You have explored {openedTips.size} of {tips.length} tips</div>
          <div className="progress" style={{ marginTop: 10 }}><span style={{ width: `${progress}%` }} /></div>
        </Card>
        <div className="segmented">
          <button className={tab === "tips" ? "active" : ""} onClick={() => setTab("tips")}>Tips</button>
          <button className={tab === "glossary" ? "active" : ""} onClick={() => setTab("glossary")}>Glossary</button>
        </div>
        {tab === "glossary" && <input className="input" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search glossary" />}
        {data.map(([title, body], index) => (
          <button key={title} className="card-button" onClick={() => tab === "tips" ? toggleTip(index) : setOpen(open === index ? null : index)}>
            <div className="row between">
              <strong>{title}</strong>
              <span className={`chevron ${open === index ? "open" : ""}`}>⌄</span>
            </div>
            <div className={`accordion-body ${open === index ? "open" : ""}`}>
              <div className="accordion-inner">
                <p className="supporting">{body}</p>
              </div>
            </div>
          </button>
        ))}
        {tab === "tips" && (
          <Card>
            <div className="title">Personalised AI Tips</div>
            <p className="supporting">Uses {formatLitres(state.entries.reduce((sum, entry) => sum + entry.waterLitres, 0))} saved and your current setup.</p>
            <Button full onClick={loadTips}>{aiLoading ? "Generating..." : aiTip ? "Regenerate" : "Get AI Tips"}</Button>
            {aiLoading && <p className="supporting">Loading tips...</p>}
            {aiTip && <p className="supporting" style={{ whiteSpace: "pre-wrap" }}>{aiTip}</p>}
          </Card>
        )}
        {tab === "glossary" && (
          <Card>
            <div className="title">Ask anything about rainwater harvesting</div>
            <div style={{ height: 12 }} />
            <input className="input" value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Ask a question" />
            <div style={{ height: 12 }} />
            <Button full onClick={askQuestion}>{qaLoading ? "Asking..." : "Ask"}</Button>
            <div className="stack" style={{ marginTop: 16 }}>
              {qaPairs.map((pair, index) => (
                <div key={`${pair.question}-${index}`}>
                  <strong>{pair.question}</strong>
                  <p className="supporting">{pair.answer}</p>
                </div>
              ))}
            </div>
          </Card>
        )}
      </main>
      <BottomNav active="tips" onChange={onTab} />
    </>
  );
}
