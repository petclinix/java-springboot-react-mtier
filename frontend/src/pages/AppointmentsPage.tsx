import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Appointment } from "../client/dto/Appointment.tsx";
import type { Pet } from "../client/dto/Pet.tsx";
import type { Vet } from "../client/dto/Vet.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Badge } from "../components/ui/Badge";
import { Input } from "../components/ui/Input";
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

export default function AppointmentsPage() {
    const client = useApiClient();

    const [appointments, setAppointments] = useState<Appointment[]>([]);
    const [pets, setPets] = useState<Pet[]>([]);
    const [vets, setVets] = useState<Vet[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [cancelling, setCancelling] = useState<number | null>(null);

    const [reschedulingId, setReschedulingId] = useState<number | null>(null);
    const [rescheduleValue, setRescheduleValue] = useState<string>("");
    const [rescheduleSubmitting, setRescheduleSubmitting] = useState(false);
    const [rescheduleError, setRescheduleError] = useState<string | null>(null);

    useEffect(() => {
        fetchAll();
    }, []);

    async function fetchAll() {
        setLoading(true);
        setError(null);
        try {
            const [appts, petsData, vetsData] = await Promise.all([
                client.listAppointments(),
                client.listPets(),
                client.listVets(),
            ]);
            setAppointments(appts);
            setPets(petsData);
            setVets(vetsData);
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
            await client.cancelAppointment(id);
            setAppointments(prev => prev.filter(a => a.id !== id));
        } catch (err: any) {
            setError(err.message || "Failed to cancel appointment");
        } finally {
            setCancelling(null);
        }
    }

    function openReschedule(id: number) {
        setReschedulingId(id);
        setRescheduleValue("");
        setRescheduleError(null);
    }

    function closeReschedule() {
        setReschedulingId(null);
        setRescheduleValue("");
        setRescheduleError(null);
    }

    async function handleRescheduleSubmit(id: number) {
        setRescheduleError(null);

        if (!rescheduleValue) {
            setRescheduleError("Please choose a date and time.");
            return;
        }
        const date = new Date(rescheduleValue);
        if (Number.isNaN(date.getTime())) {
            setRescheduleError("Invalid date/time.");
            return;
        }

        setRescheduleSubmitting(true);
        try {
            const updated = await client.rescheduleAppointment(id, date.toISOString());
            setAppointments(prev => prev.map(a => (a.id === id ? updated : a)));
            setReschedulingId(null);
            setRescheduleValue("");
        } catch (err: any) {
            setRescheduleError(err.message || "Failed to reschedule appointment");
        } finally {
            setRescheduleSubmitting(false);
        }
    }

    function petName(petId: number): string {
        return pets.find(p => p.id === petId)?.name ?? `Pet #${petId}`;
    }

    function vetName(vetId: number): string {
        return vets.find(v => v.id === vetId)?.username ?? `Vet #${vetId}`;
    }

    return (
        <PageLayout>
            <PageHeader
                title="My Appointments"
                actions={
                    <Link to="/appointments/book">
                        <Button variant="primary">+ Book appointment</Button>
                    </Link>
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
                    <EmptyState
                        message="No appointments found."
                        action={
                            <Link to="/appointments/book">
                                <Button variant="primary">Book your first appointment</Button>
                            </Link>
                        }
                    />
                )}
                <ul className="list-none p-0 m-0">
                    {appointments.map(a => {
                        const canCancel = a.status === "BOOKED" || a.status === "CONFIRMED";
                        const canReschedule = a.status === "BOOKED" || a.status === "CONFIRMED";
                        const isRescheduling = reschedulingId === a.id;
                        return (
                            <li
                                key={a.id}
                                className="flex flex-col gap-[12px] py-[12px] border-b border-border"
                            >
                                <div className="flex justify-between items-center">
                                    <div>
                                        <div className="flex items-center gap-[8px]">
                                            <strong className="text-[15px]">{a.startsAt ? new Date(a.startsAt).toLocaleString() : ""}</strong>
                                            {a.status && <Badge variant={statusBadgeVariant(a.status)}>{a.status}</Badge>}
                                        </div>
                                        <p className="mt-[4px] mb-0 text-[13px] text-muted">
                                            Pet: {petName(a.petId ?? -1)} · Vet: {vetName(a.vetId ?? -1)}
                                        </p>
                                    </div>
                                    <div className="flex gap-[8px]">
                                        {canReschedule && (
                                            <Button
                                                variant="secondary"
                                                size="sm"
                                                onClick={() => {
                                                    if (a.id === undefined) return;
                                                    if (isRescheduling) {
                                                        closeReschedule();
                                                    } else {
                                                        openReschedule(a.id);
                                                    }
                                                }}
                                            >
                                                {isRescheduling ? "Close" : "Reschedule"}
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
                                </div>

                                {isRescheduling && a.id !== undefined && (
                                    <div className="flex items-start gap-[8px] pl-[4px]">
                                        <div className="flex flex-col gap-[4px]">
                                            <Input
                                                type="datetime-local"
                                                value={rescheduleValue}
                                                onChange={(ev) => setRescheduleValue(ev.target.value)}
                                            />
                                            {rescheduleError && (
                                                <StatusMessage variant="error">{rescheduleError}</StatusMessage>
                                            )}
                                        </div>
                                        <Button
                                            variant="primary"
                                            size="sm"
                                            disabled={rescheduleSubmitting}
                                            onClick={() => handleRescheduleSubmit(a.id!)}
                                        >
                                            {rescheduleSubmitting ? "Saving…" : "Save"}
                                        </Button>
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            </Card>
        </PageLayout>
    );
}
