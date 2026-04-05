import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function VetVisitPage() {
    const { appointmentId } = useParams<{ appointmentId: string }>();
    const client = useApiClient();
    const navigate = useNavigate();

    const [vetSummary, setVetSummary] = useState("");
    const [vaccination, setVaccination] = useState("");
    const [ownerSummary, setOwnerSummary] = useState("");
    const [loading, setLoading] = useState(true);
    const [fetchError, setFetchError] = useState<string | null>(null);
    const [saveSuccess, setSaveSuccess] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    useEffect(() => {
        async function loadVisit() {
            setLoading(true);
            setFetchError(null);
            try {
                const visit = await client.getVetVisit(Number(appointmentId));
                setVetSummary(visit.vetSummary ?? "");
                setVaccination(visit.vaccination ?? "");
                setOwnerSummary(visit.ownerSummary ?? "");
            } catch (err: any) {
                setFetchError(err.message || "Failed to load visit");
            } finally {
                setLoading(false);
            }
        }
        loadVisit();
    }, [appointmentId]);

    async function handleSave() {
        setSaveSuccess(false);
        setSaveError(null);
        try {
            await client.saveVetVisit(Number(appointmentId), { vetSummary, vaccination, ownerSummary });
            setSaveSuccess(true);
        } catch (err: any) {
            setSaveError(err.message || "Failed to save visit");
        }
    }

    return (
        <PageLayout narrow>
            <PageHeader
                title="Visit Documentation"
                actions={
                    <Button variant="secondary" onClick={() => navigate("/appointments/vet")}>Back</Button>
                }
            />

            {loading && <p style={{ color: "var(--color-text-muted)" }}>Loading...</p>}

            {fetchError && <StatusMessage variant="error">{fetchError}</StatusMessage>}

            {!loading && !fetchError && (
                <Card>
                    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                            <label htmlFor="vetSummary" style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>Vet Summary</label>
                            <textarea
                                id="vetSummary"
                                value={vetSummary}
                                onChange={e => setVetSummary(e.target.value)}
                                rows={5}
                                style={{
                                    width: "100%",
                                    padding: "8px 12px",
                                    fontSize: 14,
                                    border: "1px solid var(--color-border-strong)",
                                    borderRadius: "var(--radius-md)",
                                    fontFamily: "inherit",
                                    resize: "vertical",
                                }}
                            />
                        </div>

                        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                            <label htmlFor="vaccination" style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>Vaccination</label>
                            <input
                                id="vaccination"
                                type="text"
                                value={vaccination}
                                onChange={e => setVaccination(e.target.value)}
                                style={{
                                    width: "100%",
                                    padding: "8px 12px",
                                    fontSize: 14,
                                    border: "1px solid var(--color-border-strong)",
                                    borderRadius: "var(--radius-md)",
                                    fontFamily: "inherit",
                                }}
                            />
                        </div>

                        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                            <label htmlFor="ownerSummary" style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>Owner Summary</label>
                            <textarea
                                id="ownerSummary"
                                value={ownerSummary}
                                onChange={e => setOwnerSummary(e.target.value)}
                                rows={5}
                                style={{
                                    width: "100%",
                                    padding: "8px 12px",
                                    fontSize: 14,
                                    border: "1px solid var(--color-border-strong)",
                                    borderRadius: "var(--radius-md)",
                                    fontFamily: "inherit",
                                    resize: "vertical",
                                }}
                            />
                        </div>

                        {saveSuccess && <StatusMessage variant="success">Saved successfully.</StatusMessage>}
                        {saveError && <StatusMessage variant="error">{saveError}</StatusMessage>}

                        <div style={{ display: "flex", gap: 8 }}>
                            <Button variant="primary" onClick={handleSave}>Save</Button>
                        </div>
                    </div>
                </Card>
            )}
        </PageLayout>
    );
}
