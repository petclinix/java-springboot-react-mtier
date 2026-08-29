import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { VetAppointment } from "../client/dto/VetAppointment.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Badge } from "../components/ui/Badge";
import { EmptyState } from "../components/ui/EmptyState";
import { StatusMessage } from "../components/ui/StatusMessage";

type BadgeVariant = "owner" | "vet" | "admin" | "active" | "inactive" | "neutral";

function statusBadgeVariant(status?: string): BadgeVariant {
    switch (status) {
        case "CONFIRMED":
            return "vet";
        case "COMPLETED":
            return "active";
        case "CANCELLED":
        case "NO_SHOW":
            return "inactive";
        case "BOOKED":
        default:
            return "neutral";
    }
}

export default function VetAppointmentsPage() {
    const client = useApiClient();
    const navigate = useNavigate();

    const [appointments, setAppointments] = useState<VetAppointment[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [cancelling, setCancelling] = useState<number | null>(null);
    const [confirming, setConfirming] = useState<number | null>(null);
    const [markingNoShow, setMarkingNoShow] = useState<number | null>(null);

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

    function updateStatus(id: number, status: VetAppointment["status"]) {
        setAppointments(prev => prev.map(a => (a.id === id ? { ...a, status } : a)));
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

    async function handleConfirm(id: number) {
        setConfirming(id);
        setError(null);
        try {
            await client.confirmVetAppointment(id);
            updateStatus(id, "CONFIRMED");
        } catch (err: any) {
            setError(err.message || "Failed to confirm appointment");
        } finally {
            setConfirming(null);
        }
    }

    async function handleMarkNoShow(id: number) {
        setMarkingNoShow(id);
        setError(null);
        try {
            await client.markVetAppointmentNoShow(id);
            updateStatus(id, "NO_SHOW");
        } catch (err: any) {
            setError(err.message || "Failed to mark appointment as no-show");
        } finally {
            setMarkingNoShow(null);
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
                    {appointments.map(a => {
                        const canCancel = a.status === "BOOKED" || a.status === "CONFIRMED";
                        const canConfirm = a.status === "BOOKED";
                        const canMarkNoShow = a.status === "CONFIRMED";
                        const canVisit = a.status === "CONFIRMED";
                        return (
                            <li
                                key={a.id}
                                className="flex justify-between items-center py-[12px] border-b border-border"
                            >
                                <div>
                                    <div className="flex items-center gap-[8px]">
                                        <strong className="text-[15px]">{a.startsAt ? new Date(a.startsAt).toLocaleString() : ""}</strong>
                                        {a.status && <Badge variant={statusBadgeVariant(a.status)}>{a.status}</Badge>}
                                        {a.appointmentType && <Badge variant="neutral">{a.appointmentType}</Badge>}
                                    </div>
                                    <p className="mt-[4px] mb-0 text-[13px] text-muted">
                                        Pet: {a.petName} · Owner: {a.ownerUsername}
                                    </p>
                                </div>
                                <div className="flex gap-[8px]">
                                    {canConfirm && (
                                        <Button
                                            variant="secondary"
                                            size="sm"
                                            disabled={confirming === a.id}
                                            onClick={() => a.id !== undefined && handleConfirm(a.id)}
                                        >
                                            {confirming === a.id ? "Confirming…" : "Confirm"}
                                        </Button>
                                    )}
                                    {canMarkNoShow && (
                                        <Button
                                            variant="secondary"
                                            size="sm"
                                            disabled={markingNoShow === a.id}
                                            onClick={() => a.id !== undefined && handleMarkNoShow(a.id)}
                                        >
                                            {markingNoShow === a.id ? "Marking…" : "Mark no-show"}
                                        </Button>
                                    )}
                                    {canVisit && (
                                        <Button
                                            variant="secondary"
                                            size="sm"
                                            onClick={() => navigate(`/appointments/vet/visit/${a.id}`)}
                                        >
                                            Visit
                                        </Button>
                                    )}
                                    {canCancel && (
                                        <Button
                                            variant="danger"
                                            size="sm"
                                            disabled={cancelling === a.id}
                                            onClick={() => a.id !== undefined && handleCancel(a.id)}
                                        >
                                            {cancelling === a.id ? "Cancelling…" : "Cancel"}
                                        </Button>
                                    )}
                                </div>
                            </li>
                        );
                    })}
                </ul>
            </Card>
        </PageLayout>
    );
}
