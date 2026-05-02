import type { Entry, Unit } from "../types";

export const runoffOptions = [
  { label: "Concrete", coeff: 0.85 },
  { label: "Tiled", coeff: 0.75 },
  { label: "Metal Sheet", coeff: 0.9 },
  { label: "Green Roof", coeff: 0.4 }
];

export function areaToSqFt(area: number, unit: Unit) {
  return unit === "sqm" ? area * 10.764 : area;
}

export function calculateWaterLitres(area: number, unit: Unit, rainfallMm: number, runoffCoeff: number, tankCapacity: number) {
  const raw = areaToSqFt(area, unit) * rainfallMm * 0.0929 * runoffCoeff;
  return Math.min(raw, tankCapacity);
}

export function impactDays(litres: number) {
  return litres / 135;
}

export function recalculateEntries(entries: Entry[], area: number, unit: Unit, runoffCoeff: number, tankCapacity: number) {
  return entries.map((entry) => ({
    ...entry,
    waterLitres: calculateWaterLitres(area, unit, entry.rainfallMm, runoffCoeff, tankCapacity)
  }));
}

export function formatLitres(value: number) {
  return `${Math.round(value).toLocaleString("en-IN")} L`;
}
