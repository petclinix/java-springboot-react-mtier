import React, {useEffect,  useState} from "react";
import type {Pet} from "../client/dto/Pet.tsx";
import type {Vet} from "../client/dto/Vet.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";

export default function AppointmentBookingPage() {
    const client = useApiClient();

    const [vets, setVets] = useState<Vet[] | null>(null);
    const [pets, setPets] = useState<Pet[] | null>(null);

    const [selectedVet, setSelectedVet] = useState<number| null>(null);
    const [selectedPet, setSelectedPet] = useState<number| null>(null);
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

                const vetsJson: Vet[] = await client.listVets();;
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
        <div className="max-w-2xl mx-auto p-6">
            <h1 className="text-2xl font-semibold mb-4">Book an appointment</h1>

            <div className="bg-white shadow rounded-lg p-6">
                <form onSubmit={handleSubmit}>
                    <div className="grid grid-cols-1 gap-4">
                        <label className="block">
                            <span className="text-sm font-medium">Choose a veterinarian</span>
                            <select
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-2"
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
                            </select>
                        </label>

                        <label className="block">
                            <span className="text-sm font-medium">Choose a pet</span>
                            <select
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-2"
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
                            </select>
                        </label>

                        <label className="block">
                            <span className="text-sm font-medium">Date & time</span>
                            {/* datetime-local produces a value like "2025-12-31T14:30" (local). */}
                            <input
                                type="datetime-local"
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-2"
                                value={startsAt}
                                onChange={(ev) => setStartsAt(ev.target.value)}
                            />
                            <p className="text-xs text-gray-500 mt-1">Times are interpreted in the user's local timezone.</p>
                        </label>

                        <div className="flex items-center space-x-2 mt-2">
                            <button
                                type="submit"
                                className="px-4 py-2 rounded-md bg-indigo-600 text-white font-medium hover:bg-indigo-700 disabled:opacity-60"
                                disabled={loading}
                            >
                                {loading ? "Booking…" : "Book appointment"}
                            </button>

                            <button
                                type="button"
                                className="px-3 py-2 rounded-md border"
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
                            </button>
                        </div>

                        {submitState.status === "error" && (
                            <div className="text-sm text-red-600 mt-2">{submitState.message}</div>
                        )}

                        {submitState.status === "success" && (
                            <div className="text-sm text-green-700 mt-2">{submitState.message}</div>
                        )}
                    </div>
                </form>
            </div>

        </div>
    );
}
