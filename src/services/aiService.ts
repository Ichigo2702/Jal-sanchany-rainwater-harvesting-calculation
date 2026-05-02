import type { AppState } from "../types";

type AiSetup = {
  roofArea: number;
  unit: string;
  runoffCoeff?: number;
  runoffLabel?: string;
  tankCapacity?: number;
};

type AiEntry = {
  rainfallMm?: number;
  waterLitres?: number;
};

type AiMonthlyData = {
  month?: string;
  monthKey?: string;
  rainfall?: number;
  water?: number;
  totalRainfallMm?: number;
  totalWaterSaved?: number;
};

async function sendClaudePrompt(prompt: string): Promise<string> {
  try {
    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "anthropic-version": "2023-06-01"
      },
      body: JSON.stringify({
        model: "claude-sonnet-4-20250514",
        max_tokens: 800,
        messages: [{ role: "user", content: prompt }]
      })
    });

    if (!response.ok) return "AI insight is unavailable right now. Please try again later.";

    const data = await response.json() as { content?: { type: string; text?: string }[] };
    return data.content?.find((item) => item.type === "text")?.text ?? "AI insight is unavailable right now.";
  } catch {
    return "AI insight is unavailable right now. Please try again later.";
  }
}

export async function getPersonalisedTip(state: AppState, location = "your area") {
  return [
    "AI service pending backend/API setup.",
    `Based on your ${state.roofArea} ${state.unit} roof, ${state.tankCapacity} L tank, and ${location}, clean your roof inlet and check the first-flush diverter before forecasted rain.`
  ].join(" ");
}

export function getSeasonAnalysis(setup: AiSetup, monthlyData: AiMonthlyData[], allSaved: number, impact: number): Promise<string> {
  const best = monthlyData.reduce<AiMonthlyData | undefined>((current, item) => {
    const itemLitres = item.water ?? item.totalWaterSaved ?? 0;
    const currentLitres = current ? current.water ?? current.totalWaterSaved ?? 0 : -1;
    return itemLitres > currentLitres ? item : current;
  }, undefined);
  const bestName = best?.month ?? best?.monthKey ?? "No month";
  const bestLitres = best ? best.water ?? best.totalWaterSaved ?? 0 : 0;
  const prompt = [
    "You are a water conservation advisor. Analyse this household's rainwater harvesting data.",
    `Setup: ${setup.roofArea} ${setup.unit} ${setup.runoffLabel ?? "selected"} roof, ${setup.tankCapacity ?? 0}L tank.`,
    `Monthly data: ${JSON.stringify(monthlyData)}`,
    `Total: ${allSaved}L (${impact} days). Best month: ${bestName} ${bestLitres}L.`,
    "Return:",
    "1. Two-sentence season summary.",
    "2. Three specific data-driven bullet observations.",
    "3. One forward projection to next milestone (10000/25000/50000L).",
    "Be concise and specific."
  ].join("\n");
  return sendClaudePrompt(prompt);
}

export function getTips(setup: AiSetup, entries: AiEntry[], locName: string, season: string, streak: number): Promise<string> {
  const allSaved = entries.reduce((sum, entry) => sum + (entry.waterLitres ?? 0), 0);
  const bestDay = Math.max(0, ...entries.map((entry) => entry.waterLitres ?? 0));
  const prompt = [
    "Rainwater harvesting expert. Give 4 short personalised tips for:",
    `Roof: ${setup.roofArea} ${setup.unit}, type: ${setup.runoffLabel ?? "selected roof"} (coeff: ${setup.runoffCoeff ?? 0})`,
    `Tank: ${setup.tankCapacity ?? 0}L. Location: ${locName}. Season: ${season}.`,
    `Saved: ${allSaved}L, ${entries.length} entries. Best day: ${bestDay}L. Streak: ${streak}.`,
    "Number 1-4. Max 2 sentences each. Specific with numbers.",
    "No generic advice."
  ].join("\n");
  return sendClaudePrompt(prompt);
}

export function askGlossary(question: string, setup: AiSetup): Promise<string> {
  const prompt = [
    `Answer in plain English in 3-4 sentences. User has ${setup.roofArea} ${setup.unit} ${setup.runoffLabel ?? "selected"} roof, ${setup.tankCapacity ?? 0}L tank in India.`,
    `Question: ${question}`
  ].join("\n");
  return sendClaudePrompt(prompt);
}
