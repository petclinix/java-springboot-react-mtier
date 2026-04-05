import { useEffect, useState } from "react";
import type { Stats } from "../client/dto/Stats.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { DataTable } from "../components/ui/DataTable";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function AdminDashboardPage() {
    const client = useApiClient();

    const [stats, setStats] = useState<Stats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchStats();
    }, []);

    async function fetchStats() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.getStats();
            setStats(data);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    const statItems = stats ? [
        { label: "Owners", value: stats.totalOwners },
        { label: "Vets", value: stats.totalVets },
        { label: "Pets", value: stats.totalPets },
        { label: "Appointments", value: stats.totalAppointments },
    ] : [];

    return (
        <PageLayout>
            <PageHeader title="Admin Dashboard" />

            {loading && <p style={{ color: "var(--color-text-muted)" }}>Loading...</p>}
            {error && <StatusMessage variant="error">{error}</StatusMessage>}

            {!loading && !error && stats && (
                <>
                    <div style={{ display: "flex", gap: 16, marginBottom: 32 }}>
                        {statItems.map(item => (
                            <Card key={item.label} style={{ flex: 1, textAlign: "center" }}>
                                <div style={{
                                    fontSize: "2.5rem",
                                    fontWeight: "bold",
                                    color: "var(--color-primary)",
                                    margin: "8px 0 4px",
                                }}>
                                    {item.value}
                                </div>
                                <div style={{ fontSize: "0.9rem", color: "var(--color-text-muted)" }}>
                                    {item.label}
                                </div>
                            </Card>
                        ))}
                    </div>

                    <h2 style={{ marginBottom: 12 }}>Appointments per Vet</h2>
                    <Card>
                        <DataTable
                            columns={[
                                { header: "Vet", render: row => row.vetUsername },
                                { header: "Appointments", render: row => row.count },
                            ]}
                            rows={stats.appointmentsPerVet}
                            keyFn={row => row.vetUsername}
                            emptyMessage="No appointments recorded yet."
                        />
                    </Card>
                </>
            )}
        </PageLayout>
    );
}
