import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { VetAppointment } from "../client/dto/VetAppointment.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function VetAppointmentsPage() {
    const client = useApiClient();
    const navigate = useNavigate();

    const [appointments, setAppointments] = useState<VetAppointment[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [cancelling, setCancelling] = useState<number | null>(null);

    useEffect(() => {
        fetchAppointments();
    }, []);

    async function fetchAppointments() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.listVetAppointments();
            setAppointments(data);
        } catch (err: any) {
            setError(err.message || "Failed to load appointments");
        } finally {
            setLoading(false);
        }
    }

    async function handleCancel(id: number) {
        setCancelling(id);
        setError(null);
        try {
            await client.cancelVetAppointment(id);
            setAppointments(prev => prev.filter(a => a.id !== id));
        } catch (err: any) {
            setError(err.message || "Failed to cancel appointment");
        } finally {
            setCancelling(null);
        }
    }

    return (
        <PageLayout>
            <PageHeader
                title="My Appointments"
                actions={
                    <Button variant="secondary" onClick={fetchAppointments}>Refresh</Button>
                }
            />

            {error && (
                <div className="mb-[16px]">
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}

            <Card>
                {loading && <p className="text-muted">Loading...</p>}
                {!loading && appointments.length === 0 && (
                    <EmptyState message="No appointments found." />
                )}
                <ul className="list-none p-0 m-0">
                    {appointments.map(a => (
                        <li
                            key={a.id}
                            className="flex justify-between items-center py-[12px] border-b border-border"
                        >
                            <div>
                                <strong className="text-[15px]">{new Date(a.startsAt).toLocaleString()}</strong>
                                <p className="mt-[4px] mb-0 text-[13px] text-muted">
                                    Pet: {a.petName} · Owner: {a.ownerUsername}
                                </p>
                            </div>
                            <div className="flex gap-[8px]">
                                <Button
                                    variant="secondary"
                                    size="sm"
                                    onClick={() => navigate(`/appointments/vet/visit/${a.id}`)}
                                >
                                    Visit
                                </Button>
                                <Button
                                    variant="danger"
                                    size="sm"
                                    disabled={cancelling === a.id}
                                    onClick={() => handleCancel(a.id)}
                                >
                                    {cancelling === a.id ? "Cancelling…" : "Cancel"}
                                </Button>
                            </div>
                        </li>
                    ))}
                </ul>
            </Card>
        </PageLayout>
    );
}
