import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useApiClient } from "../hooks/useApiClient.ts";
import type { OwnerVisit } from "../client/dto/OwnerVisit.tsx";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function PetVisitsPage() {
    const { petId } = useParams<{ petId: string }>();
    const client = useApiClient();
    const navigate = useNavigate();

    const [visits, setVisits] = useState<OwnerVisit[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function loadVisits() {
            setLoading(true);
            setError(null);
            try {
                const data = await client.listPetVisits(Number(petId));
                setVisits(data);
            } catch (err: any) {
                setError(err.message || "Failed to load visits");
            } finally {
                setLoading(false);
            }
        }
        loadVisits();
    }, [petId]);

    return (
        <PageLayout>
            <PageHeader
                title="Pet Visits"
                actions={
                    <Button variant="secondary" onClick={() => navigate("/pets")}>Back</Button>
                }
            />

            {loading && <p style={{ color: "var(--color-text-muted)" }}>Loading...</p>}

            {error && <StatusMessage variant="error">{error}</StatusMessage>}

            {!loading && !error && (
                <Card>
                    {visits.length === 0 ? (
                        <EmptyState message="No visits found." />
                    ) : (
                        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                            {visits.map(v => (
                                <li
                                    key={v.id}
                                    style={{
                                        padding: "12px 0",
                                        borderBottom: "1px solid var(--color-border)",
                                    }}
                                >
                                    <div>
                                        <strong style={{ fontSize: 15 }}>{new Date(v.startsAt).toLocaleString()}</strong>
                                        <p style={{ margin: "4px 0 0", fontSize: 13, color: "var(--color-text-muted)" }}>
                                            Vet: <span>{v.vetUsername}</span>
                                        </p>
                                        <p style={{ margin: "4px 0 0", fontSize: 13 }}>
                                            <strong>Owner Summary:</strong> <span>{v.ownerSummary ?? "—"}</span>
                                        </p>
                                        <p style={{ margin: "4px 0 0", fontSize: 13 }}>
                                            <strong>Vaccination:</strong> <span>{v.vaccination ?? "—"}</span>
                                        </p>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </Card>
            )}
        </PageLayout>
    );
}
