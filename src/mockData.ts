import type { AppState } from "./types";

export const INIT_ENTRIES = [
  { id: 1, date: "2025-05-10", mm: 8, litres: 504.7 },
  { id: 2, date: "2025-05-18", mm: 14, litres: 882.5 },
  { id: 3, date: "2025-06-05", mm: 28, litres: 1765.0 },
  { id: 4, date: "2025-06-12", mm: 35, litres: 2206.3 },
  { id: 5, date: "2025-06-20", mm: 22, litres: 1386.9 },
  { id: 6, date: "2025-06-28", mm: 41, litres: 2584.7 },
  { id: 7, date: "2025-07-03", mm: 52, litres: 3000.0 },
  { id: 8, date: "2025-07-09", mm: 38, litres: 2395.7 },
  { id: 9, date: "2025-07-15", mm: 60, litres: 3000.0 },
  { id: 10, date: "2025-07-22", mm: 45, litres: 2836.8 },
  { id: 11, date: "2025-08-02", mm: 48, litres: 3000.0 },
  { id: 12, date: "2025-08-11", mm: 55, litres: 3000.0 },
  { id: 13, date: "2025-08-19", mm: 32, litres: 2017.2 },
  { id: 14, date: "2025-08-27", mm: 40, litres: 2521.6 },
  { id: 15, date: "2025-09-04", mm: 30, litres: 1891.2 },
  { id: 16, date: "2025-09-15", mm: 18, litres: 1134.7 },
  { id: 17, date: "2025-09-24", mm: 22, litres: 1386.9 },
  { id: 18, date: "2025-10-08", mm: 25, litres: 1576.0 },
  { id: 19, date: "2025-10-20", mm: 15, litres: 945.6 },
  { id: 20, date: "2025-11-05", mm: 20, litres: 1260.8 },
  { id: 21, date: "2025-11-18", mm: 12, litres: 756.5 },
  { id: 22, date: "2025-12-10", mm: 6, litres: 378.2 },
  { id: 23, date: "2026-01-14", mm: 4, litres: 252.2 },
  { id: 24, date: "2026-02-20", mm: 10, litres: 630.4 },
  { id: 25, date: "2026-03-08", mm: 16, litres: 1008.6 },
  { id: 26, date: "2026-04-12", mm: 18, litres: 1134.7 },
  { id: 27, date: "2026-04-28", mm: 12, litres: 756.5 },
  { id: 28, date: "2026-04-29", mm: 0, litres: 0.0 },
  { id: 29, date: "2026-04-30", mm: 25, litres: 1576.0 }
];

export const initialAppState: AppState = {
  setupDone: false,
  roofArea: 800,
  unit: "sqft",
  tankCapacity: 3000,
  runoffCoeff: 0.85,
  entries: INIT_ENTRIES.map((entry) => ({ id: String(entry.id), date: entry.date, rainfallMm: entry.mm, waterLitres: entry.litres })),
  theme: "light",
  amoled: false
};

export const mockMonthlyData = [
  { month: "May", rainfall: 18, water: 1224 },
  { month: "Jun", rainfall: 72, water: 4892 },
  { month: "Jul", rainfall: 96, water: 6522 },
  { month: "Aug", rainfall: 84, water: 5707 },
  { month: "Sep", rainfall: 68, water: 4620 },
  { month: "Oct", rainfall: 40, water: 2718 },
  { month: "Nov", rainfall: 62, water: 4210 },
  { month: "Dec", rainfall: 48, water: 3260 },
  { month: "Jan", rainfall: 22, water: 1490 },
  { month: "Feb", rainfall: 36, water: 2440 },
  { month: "Mar", rainfall: 55, water: 3730 },
  { month: "Apr", rainfall: 37, water: 2455 }
];
