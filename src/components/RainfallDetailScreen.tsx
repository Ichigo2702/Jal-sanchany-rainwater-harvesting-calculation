import type { Entry } from "../types";
import { formatLitres, impactDays } from "../services/calculations";
import { Button, Card, Header, Icon } from "./UI";

export function RainfallDetailScreen({ entry, onBack, onEdit, onDelete }: { entry: Entry; onBack: () => void; onEdit: () => void; onDelete: () => void }) {
  return (
    <>
      <Header title="Rainfall Details" subtitle={entry.date} onBack={onBack} />
      <main className="content stack">
        <Card>
          <div className="key-metric">{entry.rainfallMm} mm</div>
          <div className="title">Rainfall recorded</div>
          <p className="supporting">{formatLitres(entry.waterLitres)} collected</p>
        </Card>
        <Card>
          <div className="title">{impactDays(entry.waterLitres).toFixed(1)} days</div>
          <p className="supporting">Days of water supply</p>
          <div className="subtle-pill"><Icon name="info" /> Based on roof area and runoff coefficient</div>
        </Card>
      </main>
      <footer className="actions row">
        <Button variant="secondary" full onClick={onEdit}>Edit</Button>
        <Button variant="danger" full onClick={onDelete}>Delete</Button>
      </footer>
    </>
  );
}
