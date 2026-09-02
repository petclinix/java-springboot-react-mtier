import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Appointment } from "../client/dto/Appointment.tsx";
import type { Pet } from "../client/dto/Pet.tsx";
import type { Vet } from "../client/dto/Vet.tsx";
import type { AvailableSlot } from "../client/dto/AvailableSlot.ts";
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
    const [rescheduleDate, setRescheduleDate] = useState<string>(""); // value for input type="date"
    const [rescheduleSlots, setRescheduleSlots] = useState<AvailableSlot[] | null>(null);
    const [rescheduleSlotsLoading, setRescheduleSlotsLoading] = useState(false);
    const [rescheduleSlotsError, setRescheduleSlotsError] = useState<string | null>(null);
    const [selectedRescheduleSlot, setSelectedRescheduleSlot] = useState<AvailableSlot | null>(null);
    const [rescheduleSubmitting, setRescheduleSubmitting] = useState(false);
    const [rescheduleError, setRescheduleError] = useState<string | null>(null);

    const fetchAll = useCallback(async () => {
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
        } catch (err) {
            setError((err instanceof Error ? err.message : undefined) || "Failed to load appointments");
        } finally {
            setLoading(false);
        }
    }, [client]);

    useEffect(() => {
        fetchAll();
    }, [fetchAll]);

    async function handleCancel(id: number) {
        setCancelling(id);
        setError(null);
        try {
            await client.cancelAppointment(id);
            setAppointments(prev => prev.filter(a => a.id !== id));
        } catch (err) {
            setError((err instanceof Error ? err.message : undefined) || "Failed to cancel appointment");
        } finally {
            setCancelling(null);
        }
    }

    function openReschedule(id: number) {
        setReschedulingId(id);
        setRescheduleDate("");
        setRescheduleSlots(null);
        setRescheduleSlotsError(null);
        setSelectedRescheduleSlot(null);
        setRescheduleError(null);
    }

    function closeReschedule() {
        setReschedulingId(null);
        setRescheduleDate("");
        setRescheduleSlots(null);
        setRescheduleSlotsError(null);
        setSelectedRescheduleSlot(null);
        setRescheduleError(null);
    }

    // Re-fetch available slots for the appointment being rescheduled whenever the chosen date changes
    useEffect(() => {
        let cancelled = false;
        setSelectedRescheduleSlot(null);

        if (reschedulingId == null || !rescheduleDate) {
            setRescheduleSlots(null);
            setRescheduleSlotsError(null);
            return;
        }

        const appointment = appointments.find(a => a.id === reschedulingId);
        if (!appointment || appointment.locationId == null || !appointment.appointmentType) {
            setRescheduleSlots(null);
            setRescheduleSlotsError(null);
            return;
        }

        async function fetchSlots() {
            setRescheduleSlotsLoading(true);
            setRescheduleSlotsError(null);
            try {
                const data = await client.listAvailableSlots(
                    appointment!.locationId!,
                    rescheduleDate,
                    appointment!.appointmentType!
                );
                if (!cancelled) setRescheduleSlots(data);
            } catch (err) {
                if (!cancelled) setRescheduleSlotsError((err as Error).message || "Failed to load available slots");
            } finally {
                if (!cancelled) setRescheduleSlotsLoading(false);
            }
        }

        fetchSlots();

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [reschedulingId, rescheduleDate]);

    async function handleRescheduleSubmit(id: number) {
        setRescheduleError(null);

        if (!selectedRescheduleSlot || !selectedRescheduleSlot.startsAt) {
            setRescheduleError("Please choose an available time slot.");
            return;
        }

        setRescheduleSubmitting(true);
        try {
            const updated = await client.rescheduleAppointment(id, selectedRescheduleSlot.startsAt);
            setAppointments(prev => prev.map(a => (a.id === id ? updated : a)));
            closeReschedule();
        } catch (err) {
            setRescheduleError((err instanceof Error ? err.message : undefined) || "Failed to reschedule appointment");
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

    function formatSlotTime(slot: AvailableSlot): string {
        return slot.startsAt ? new Date(slot.startsAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "";
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
                                            {a.appointmentType && <Badge variant="neutral">{a.appointmentType}</Badge>}
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
                                    <div className="flex flex-col gap-[8px] pl-[4px]">
                                        <Input
                                            type="date"
                                            value={rescheduleDate}
                                            onChange={(ev) => setRescheduleDate(ev.target.value)}
                                        />

                                        {rescheduleDate && (
                                            <>
                                                {rescheduleSlotsLoading && <p className="text-muted">Loading slots...</p>}
                                                {!rescheduleSlotsLoading && rescheduleSlotsError && (
                                                    <StatusMessage variant="error">{rescheduleSlotsError}</StatusMessage>
                                                )}
                                                {!rescheduleSlotsLoading && !rescheduleSlotsError && rescheduleSlots && rescheduleSlots.length === 0 && (
                                                    <EmptyState message="No available slots for this day — try another date." />
                                                )}
                                                {!rescheduleSlotsLoading && !rescheduleSlotsError && rescheduleSlots && rescheduleSlots.length > 0 && (
                                                    <div className="flex flex-wrap gap-[8px]">
                                                        {rescheduleSlots.map((slot) => {
                                                            const isSelected = selectedRescheduleSlot?.startsAt === slot.startsAt;
                                                            return (
                                                                <Button
                                                                    key={slot.startsAt}
                                                                    type="button"
                                                                    variant={isSelected ? "primary" : "secondary"}
                                                                    size="sm"
                                                                    onClick={() => setSelectedRescheduleSlot(slot)}
                                                                >
                                                                    {formatSlotTime(slot)}
                                                                </Button>
                                                            );
                                                        })}
                                                    </div>
                                                )}
                                            </>
                                        )}

                                        {rescheduleError && (
                                            <StatusMessage variant="error">{rescheduleError}</StatusMessage>
                                        )}

                                        <div>
                                            <Button
                                                variant="primary"
                                                size="sm"
                                                disabled={rescheduleSubmitting}
                                                onClick={() => handleRescheduleSubmit(a.id!)}
                                            >
                                                {rescheduleSubmitting ? "Saving…" : "Save"}
                                            </Button>
                                        </div>
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
