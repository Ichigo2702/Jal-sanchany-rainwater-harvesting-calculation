export type Unit = "sqft" | "sqm";
export type Screen = "splash" | "onboarding" | "dashboard" | "reports" | "history" | "tips" | "settings" | "rainfallEntry" | "rainfallDetail";
export type MainTab = "dashboard" | "reports" | "history" | "tips" | "settings";
export type ThemeMode = "light" | "dark";

export type Entry = {
  id: string;
  date: string;
  rainfallMm: number;
  waterLitres: number;
};

export type RunoffOption = {
  label: string;
  coeff: number;
};

export type AppState = {
  setupDone: boolean;
  roofArea: number;
  unit: Unit;
  tankCapacity: number;
  runoffCoeff: number;
  entries: Entry[];
  theme: ThemeMode;
  amoled: boolean;
};

export type ForecastState =
  | { status: "idle"; location: string; message?: string }
  | { status: "loading"; location: string; message?: string }
  | { status: "success"; location: string; rainfallMm: number; message: string }
  | { status: "error"; location: string; message: string };
