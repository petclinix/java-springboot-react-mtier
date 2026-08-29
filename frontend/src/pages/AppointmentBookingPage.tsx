import React, { useEffect, useState } from "react";
import type { Pet } from "../client/dto/Pet.tsx";
import type { BookableLocation } from "../client/dto/BookableLocation.ts";
import type { AvailableSlot } from "../client/dto/AvailableSlot.ts";
import type { AppointmentRequest } from "../client/dto/AppointmentRequest.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { FormField } from "../components/ui/FormField";
import { Select } from "../components/ui/Select";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { StatusMessage } from "../components/ui/StatusMessage";

const APPOINTMENT_TYPES = ["VACCINATION", "FOLLOW_UP", "CHECKUP", "EMERGENCY", "SURGERY"];

export default function AppointmentBookingPage() {
    const client = useApiClient();

    const [locations, setLocations] = useState<BookableLocation[] | null>(null);
    const [pets, setPets] = useState<Pet[] | null>(null);

    const [selectedLocation, setSelectedLocation] = useState<number | null>(null);
    const [selectedPet, setSelectedPet] = useState<number | null>(null);
    const [appointmentType, setAppointmentType] = useState<string>(APPOINTMENT_TYPES[0]);
    const [date, setDate] = useState<string>(""); // value for input type="date"

    const [slots, setSlots] = useState<AvailableSlot[] | null>(null);
    const [slotsLoading, setSlotsLoading] = useState(false);
    const [slotsError, setSlotsError] = useState<string | null>(null);
    const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null);

    const [loading, setLoading] = useState(false);
    const [submitState, setSubmitState] = useState<{ status: "idle" | "success" | "error"; message?: string }>(
        { status: "idle" }
    );

    // Fetch locations and pets on mount
    useEffect(() => {
        let cancelled = false;

        async function fetchLists() {
            try {
                setLoading(true);

                const locationsJson: BookableLocation[] = await client.listBookableLocations();
                const petsJson: Pet[] = await client.listPets();

                if (!cancelled) {
                    setLocations(locationsJson);
                    setPets(petsJson);
                    // Preselect first items if available
                    if (locationsJson.length > 0) setSelectedLocation(Number(locationsJson[0].id));
                    if (petsJson.length > 0) setSelectedPet(Number(petsJson[0].id));
                }
            } catch (err) {
                console.error(err);
                if (!cancelled) setSubmitState({ status: "error", message: (err as Error).message });
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        fetchLists();

        return () => {
            cancelled = true;
        };
    }, []);

    // Re-fetch available slots whenever location, appointment type, or date changes
    useEffect(() => {
        let cancelled = false;
        setSelectedSlot(null);

        if (!selectedLocation || !appointmentType || !date) {
            setSlots(null);
            setSlotsError(null);
            return;
        }

        async function fetchSlots() {
            setSlotsLoading(true);
            setSlotsError(null);
            try {
                const data = await client.listAvailableSlots(selectedLocation!, date, appointmentType);
                if (!cancelled) setSlots(data);
            } catch (err) {
                if (!cancelled) setSlotsError((err as Error).message || "Failed to load available slots");
            } finally {
                if (!cancelled) setSlotsLoading(false);
            }
        }

        fetchSlots();

        return () => {
            cancelled = true;
        };
    }, [selectedLocation, appointmentType, date]);

    function formatSlotTime(slot: AvailableSlot): string {
        return slot.startsAt ? new Date(slot.startsAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "";
    }

    function validate(): string | null {
        if (!selectedLocation) return "Please choose a location.";
        if (!selectedPet) return "Please choose a pet.";
        if (!appointmentType) return "Please choose an appointment type.";
        if (!date) return "Please choose a date.";
        if (!selectedSlot) return "Please choose an available time slot.";

        return null;
    }

    async function handleSubmit(e?: React.FormEvent) {
        if (e) e.preventDefault();
        setSubmitState({ status: "idle" });

        const err = validate();
        if (err) {
            setSubmitState({ status: "error", message: err });
            return;
        }

        try {
            setLoading(true);
            const created = await client.createAppointment({
                locationId: selectedLocation!,
                petId: selectedPet!,
                // appointmentType is a plain string here (only ever populated from APPOINTMENT_TYPES via
                // the dropdown), narrowed to AppointmentRequest's literal union.
                appointmentType: appointmentType as AppointmentRequest["appointmentType"],
                startsAt: selectedSlot!.startsAt!,
            });
            setSubmitState({ status: "success", message: `Appointment created (id: ${created.id ?? "n/a"})` });

            // Reset the date/slot selection only
            setDate("");
            setSlots(null);
            setSelectedSlot(null);
        } catch (err) {
            console.error(err);
            setSubmitState({ status: "error", message: (err as Error).message });
        } finally {
            setLoading(false);
        }
    }

    return (
        <PageLayout narrow>
            <PageHeader title="Book an appointment" />
            <Card>
                <form onSubmit={handleSubmit} className="flex flex-col gap-[16px]">
                    <FormField label="Choose a location">
                        <Select
                            value={selectedLocation?.toString()}
                            onChange={(ev) => setSelectedLocation(Number(ev.target.value))}
                            disabled={!!loading || !locations}
                        >
                            {locations && locations.length > 0 ? (
                                locations.map((loc) => (
                                    <option key={loc.id} value={loc.id}>
                                        {loc.name} — {loc.vetUsername}
                                    </option>
                                ))
                            ) : (
                                <option value="">No locations available</option>
                            )}
                        </Select>
                    </FormField>

                    <FormField label="Choose a pet">
                        <Select
                            value={selectedPet?.toString()}
                            onChange={(ev) => setSelectedPet(Number(ev.target.value))}
                            disabled={!!loading || !pets}
                        >
                            {pets && pets.length > 0 ? (
                                pets.map((p) => (
                                    <option key={p.id} value={p.id}>
                                        {p.name}{p.species ? ` — ${p.species}` : ""}
                                    </option>
                                ))
                            ) : (
                                <option value="">No pets available</option>
                            )}
                        </Select>
                    </FormField>

                    <FormField label="Appointment type">
                        <Select
                            value={appointmentType}
                            onChange={(ev) => setAppointmentType(ev.target.value)}
                            disabled={!!loading}
                        >
                            {APPOINTMENT_TYPES.map((type) => (
                                <option key={type} value={type}>
                                    {type}
                                </option>
                            ))}
                        </Select>
                    </FormField>

                    <FormField label="Date">
                        <Input
                            type="date"
                            value={date}
                            onChange={(ev) => setDate(ev.target.value)}
                        />
                    </FormField>

                    {date && (
                        <FormField label="Available slots">
                            {slotsLoading && <p className="text-muted">Loading slots...</p>}
                            {!slotsLoading && slotsError && (
                                <StatusMessage variant="error">{slotsError}</StatusMessage>
                            )}
                            {!slotsLoading && !slotsError && slots && slots.length === 0 && (
                                <EmptyState message="No available slots for this day — try another date." />
                            )}
                            {!slotsLoading && !slotsError && slots && slots.length > 0 && (
                                <div className="flex flex-wrap gap-[8px]">
                                    {slots.map((slot) => {
                                        const isSelected = selectedSlot?.startsAt === slot.startsAt;
                                        return (
                                            <Button
                                                key={slot.startsAt}
                                                type="button"
                                                variant={isSelected ? "primary" : "secondary"}
                                                size="sm"
                                                onClick={() => setSelectedSlot(slot)}
                                            >
                                                {formatSlotTime(slot)}
                                            </Button>
                                        );
                                    })}
                                </div>
                            )}
                        </FormField>
                    )}

                    <div className="flex items-center gap-[8px]">
                        <Button type="submit" variant="primary" loading={loading}>
                            {loading ? "Booking…" : "Book appointment"}
                        </Button>
                    </div>

                    {submitState.status === "error" && (
                        <StatusMessage variant="error">{submitState.message}</StatusMessage>
                    )}

                    {submitState.status === "success" && (
                        <StatusMessage variant="success">{submitState.message}</StatusMessage>
                    )}
                </form>
            </Card>
        </PageLayout>
    );
}
