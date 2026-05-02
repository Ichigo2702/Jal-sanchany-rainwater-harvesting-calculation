import type { MainTab } from "../types";

type IconName = "home" | "chart" | "history" | "tips" | "settings" | "back" | "plus" | "trash" | "edit" | "info" | "download" | "upload" | "location" | "moon";

const paths: Record<IconName, string> = {
  home: "M4 11.5 12 5l8 6.5V20a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1v-8.5Z",
  chart: "M5 19V9m7 10V5m7 14v-7",
  history: "M12 8v5l3 2m6-3a9 9 0 1 1-3-6.7",
  tips: "M9 18h6M10 22h4M8.5 14.5a6 6 0 1 1 7 0c-.7.5-1.1 1.3-1.2 2.1H9.7c-.1-.8-.5-1.6-1.2-2.1Z",
  settings: "M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Zm8-3.5 1.5-1.2-1.5-2.6-1.9.8a7 7 0 0 0-1.4-.8L16.5 6h-3l-.3 2.2c-.5.2-1 .5-1.4.8L10 8.2l-1.5 2.6L10 12c0 .3-.1.7-.1 1s0 .7.1 1l-1.5 1.2 1.5 2.6 1.9-.8c.4.3.9.6 1.4.8l.3 2.2h3l.3-2.2c.5-.2 1-.5 1.4-.8l1.9.8 1.5-2.6L20 14c0-.3.1-.7.1-1s0-.7-.1-1Z",
  back: "M15 6 9 12l6 6",
  plus: "M12 5v14M5 12h14",
  trash: "M5 7h14M10 11v6m4-6v6M8 7l1-3h6l1 3m-9 0 1 14h8l1-14",
  edit: "M4 20h4L18.5 9.5a2.1 2.1 0 0 0-3-3L5 17v3Z",
  info: "M12 17v-6m0-4h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z",
  download: "M12 4v10m0 0 4-4m-4 4-4-4M5 20h14",
  upload: "M12 20V10m0 0 4 4m-4-4-4 4M5 4h14",
  location: "M12 21s7-5.2 7-12a7 7 0 1 0-14 0c0 6.8 7 12 7 12Zm0-9a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z",
  moon: "M21 14.5A8 8 0 0 1 9.5 3a9 9 0 1 0 11.5 11.5Z"
};

export function Icon({ name }: { name: IconName }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={paths[name]} />
    </svg>
  );
}

export function Header({ title, subtitle, onBack }: { title: string; subtitle?: string; onBack?: () => void }) {
  return (
    <header className="header">
      {onBack && (
        <button className="icon-btn" onClick={onBack} aria-label="Go back">
          <Icon name="back" />
        </button>
      )}
      <div>
        <div className="header-title">{title}</div>
        {subtitle && <div className="header-subtitle">{subtitle}</div>}
      </div>
    </header>
  );
}

export function Button({ children, onClick, variant = "primary", full = false, type = "button", disabled = false }: { children: React.ReactNode; onClick?: () => void; variant?: "primary" | "secondary" | "danger"; full?: boolean; type?: "button" | "submit"; disabled?: boolean }) {
  return (
    <button className={`btn ${variant} ${full ? "full" : ""}`} onClick={onClick} type={type} disabled={disabled}>
      {children}
    </button>
  );
}

export function Card({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <section className={`card ${className}`}>{children}</section>;
}

const navItems: { id: MainTab; label: string; icon: IconName }[] = [
  { id: "dashboard", label: "Home", icon: "home" },
  { id: "reports", label: "Reports", icon: "chart" },
  { id: "history", label: "History", icon: "history" },
  { id: "tips", label: "Tips", icon: "tips" },
  { id: "settings", label: "Settings", icon: "settings" }
];

export function BottomNav({ active, onChange }: { active: MainTab; onChange: (tab: MainTab) => void }) {
  return (
    <nav className="bottom-nav">
      {navItems.map((item) => (
        <button key={item.id} className={`nav-btn ${active === item.id ? "active" : ""}`} onClick={() => onChange(item.id)}>
          <Icon name={item.icon} />
          {item.label}
        </button>
      ))}
    </nav>
  );
}

export function Segmented<T extends string>({ value, options, onChange }: { value: T; options: { label: string; value: T }[]; onChange: (value: T) => void }) {
  return (
    <div className="segmented">
      {options.map((option) => (
        <button key={option.value} className={value === option.value ? "active" : ""} onClick={() => onChange(option.value)} type="button">
          {option.label}
        </button>
      ))}
    </div>
  );
}

export function Toast({ message }: { message?: string }) {
  if (!message) return null;
  return <div className="toast">{message}</div>;
}

export function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel }: { title: string; message: string; confirmLabel: string; onConfirm: () => void; onCancel: () => void }) {
  return (
    <div className="dialog-backdrop">
      <div className="dialog">
        <div className="title">{title}</div>
        <p className="supporting">{message}</p>
        <div className="row" style={{ justifyContent: "flex-end" }}>
          <Button variant="secondary" onClick={onCancel}>Cancel</Button>
          <Button variant="danger" onClick={onConfirm}>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}
