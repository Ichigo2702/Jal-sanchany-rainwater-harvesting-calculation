export function SplashScreen() {
  return (
    <main className="content" style={{ display: "grid", placeItems: "center", minHeight: "100vh", textAlign: "center" }}>
      <div className="stack">
        <div style={{ width: 92, height: 92, borderRadius: 24, background: "var(--primary-soft)", display: "grid", placeItems: "center", margin: "0 auto", color: "var(--primary)" }}>
          <svg width="52" height="52" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M12 2.5 6.4 9.2a8.1 8.1 0 1 0 11.2 0L12 2.5Z" />
          </svg>
        </div>
        <div>
          <div className="title">Jal-Sanchay Tracker</div>
          <div className="supporting">Every drop counted.</div>
        </div>
      </div>
    </main>
  );
}
