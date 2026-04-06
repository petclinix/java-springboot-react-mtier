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

            {loading && <p className="text-muted">Loading...</p>}
            {error && <StatusMessage variant="error">{error}</StatusMessage>}

            {!loading && !error && stats && (
                <>
                    <div className="flex gap-[16px] mb-[32px]">
                        {statItems.map(item => (
                            <Card key={item.label} className="flex-1 text-center">
                                <div className="text-[2.5rem] font-bold text-primary mt-[8px] mb-[4px]">
                                    {item.value}
                                </div>
                                <div className="text-[0.9rem] text-muted">
                                    {item.label}
                                </div>
                            </Card>
                        ))}
                    </div>

                    <h2 className="mb-[12px]">Appointments per Vet</h2>
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
