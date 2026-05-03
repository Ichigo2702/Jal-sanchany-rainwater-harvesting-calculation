import type { AppState } from "./types";

export const initialAppState: AppState = {
  setupDone: false,
  roofArea: 800,
  unit: "sqft",
  tankCapacity: 3000,
  runoffCoeff: 0.85,
  entries: [],
  theme: "light",
  amoled: false
};
