import React, { useEffect, useState } from "react";
import type { Pet } from "../client/dto/Pet.tsx";
import type { Vet } from "../client/dto/Vet.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { FormField } from "../components/ui/FormField";
import { Select } from "../components/ui/Select";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function AppointmentBookingPage() {
    const client = useApiClient();

    const [vets, setVets] = useState<Vet[] | null>(null);
    const [pets, setPets] = useState<Pet[] | null>(null);

    const [selectedVet, setSelectedVet] = useState<number | null>(null);
    const [selectedPet, setSelectedPet] = useState<number | null>(null);
    const [startsAt, setStartsAt] = useState<string>(""); // value for input datetime-local

    const [loading, setLoading] = useState(false);
    const [submitState, setSubmitState] = useState<{ status: "idle" | "success" | "error"; message?: string }>(
        { status: "idle" }
    );

    // Fetch vets and pets on mount
    useEffect(() => {
        let cancelled = false;

        async function fetchLists() {
            try {
                setLoading(true);

                const vetsJson: Vet[] = await client.listVets();
                const petsJson: Pet[] = await client.listPets();

                if (!cancelled) {
                    setVets(vetsJson);
                    setPets(petsJson);
                    // Preselect first items if available
                    if (vetsJson.length > 0) setSelectedVet(Number(vetsJson[0].id));
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

    function validate(): string | null {
        if (!selectedVet) return "Please choose a vet.";
        if (!selectedPet) return "Please choose a pet.";
        if (!startsAt) return "Please choose a date and time.";

        // Ensure startsAt is a valid ISO when converted
        const date = new Date(startsAt);
        if (Number.isNaN(date.getTime())) return "Invalid date/time.";

        // Optional: ensure date is in the future
        if (date.getTime() <= Date.now()) return "Please choose a future date/time.";

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
                vetId: selectedVet!,
                petId: selectedPet!,
                // Convert from the datetime-local value (which is local) to an ISO string
                startsAt: new Date(startsAt).toISOString(),
            });
            setSubmitState({ status: "success", message: `Appointment created (id: ${created.id ?? "n/a"})` });

            // Optionally: reset the date field only
            setStartsAt("");
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
                <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                    <FormField label="Choose a veterinarian">
                        <Select
                            value={selectedVet?.toString()}
                            onChange={(ev) => setSelectedVet(Number(ev.target.value))}
                            disabled={!!loading || !vets}
                        >
                            {vets && vets.length > 0 ? (
                                vets.map((v) => (
                                    <option key={v.id} value={v.id}>
                                        {v.username}
                                    </option>
                                ))
                            ) : (
                                <option value="">No vets available</option>
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

                    <FormField label="Date & time" hint="Times are interpreted in the user's local timezone.">
                        {/* datetime-local produces a value like "2025-12-31T14:30" (local). */}
                        <Input
                            type="datetime-local"
                            value={startsAt}
                            onChange={(ev) => setStartsAt(ev.target.value)}
                        />
                    </FormField>

                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <Button type="submit" variant="primary" loading={loading}>
                            {loading ? "Booking…" : "Book appointment"}
                        </Button>

                        <Button
                            type="button"
                            variant="secondary"
                            onClick={() => {
                                // Quick example: prefill with tomorrow at 10:00
                                const d = new Date();
                                d.setDate(d.getDate() + 1);
                                d.setHours(10, 0, 0, 0);
                                // Convert to YYYY-MM-DDThh:mm for datetime-local
                                const isoLocal = d.toISOString();
                                const local = isoLocal.substring(0, 16);
                                setStartsAt(local);
                            }}
                        >
                            Prefill: tomorrow 10:00
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
