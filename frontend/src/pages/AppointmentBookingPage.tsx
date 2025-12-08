import React, { useEffect, useState} from "react";
import {useAuth} from "../context/AuthContext.tsx";

/**
 * AppointmentBookingPage.tsx
 * A simple, single-file React + TypeScript page to book an appointment with a selectable vet for a single pet.
 * - Uses real API endpoints:
 *   GET  /api/vets        -> [{ id, name, specialty? }]
 *   GET  /api/pets        -> [{ id, name, species? }]
 *   POST /api/appointments -> { vetId, petId, startsAt }
 *
 * Tailwind CSS classes are used for styling. Default export is the page component.
 */

type Vet = {
    id: string | number;
    name: string;
    specialty?: string;
};

type Pet = {
    id: string | number;
    name: string;
    species?: string;
};

type AppointmentRequest = {
    vetId: string | number;
    petId: string | number;
    startsAt: string; // ISO string
};

export default function AppointmentBookingPage() {
    const [vets, setVets] = useState<Vet[] | null>(null);
    const [pets, setPets] = useState<Pet[] | null>(null);

    const [selectedVet, setSelectedVet] = useState<string | number | "">("");
    const [selectedPet, setSelectedPet] = useState<string | number | "">("");
    const [startsAt, setStartsAt] = useState<string>(""); // value for input datetime-local

    const [loading, setLoading] = useState(false);
    const [submitState, setSubmitState] = useState<{ status: "idle" | "success" | "error"; message?: string }>(
        { status: "idle" }
    );

    const { token } = useAuth();

    // Fetch vets and pets on mount
    useEffect(() => {
        let cancelled = false;

        async function fetchLists() {
            try {
                setLoading(true);
                const [vetsRes, petsRes] = await Promise.all([
                    fetch("/api/vets", {
                        headers: {
                            "Authorization": `Bearer ${token}`
                        },
                    }),
                    fetch("/api/pets", {
                        headers: {
                            "Authorization": `Bearer ${token}`
                        },
                    }),
                ]);

                if (!vetsRes.ok) throw new Error("Failed to load vets");
                if (!petsRes.ok) throw new Error("Failed to load pets");

                const vetsJson: Vet[] = await vetsRes.json();
                const petsJson: Pet[] = await petsRes.json();

                if (!cancelled) {
                    setVets(vetsJson);
                    setPets(petsJson);
                    // Preselect first items if available
                    if (vetsJson.length > 0) setSelectedVet(vetsJson[0].id);
                    if (petsJson.length > 0) setSelectedPet(petsJson[0].id);
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

        const payload: AppointmentRequest = {
            vetId: selectedVet as string | number,
            petId: selectedPet as string | number,
            // Convert from the datetime-local value (which is local) to an ISO string
            startsAt: new Date(startsAt).toISOString(),
        };

        try {
            setLoading(true);
            const res = await fetch("/api/appointments", {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Server returned ${res.status}`);
            }

            const created = await res.json();
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
                                value={selectedVet}
                                onChange={(ev) => setSelectedVet(ev.target.value)}
                                disabled={!!loading || !vets}
                            >
                                {vets && vets.length > 0 ? (
                                    vets.map((v) => (
                                        <option key={v.id} value={v.id}>
                                            {v.name}{v.specialty ? ` — ${v.specialty}` : ""}
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
                                value={selectedPet}
                                onChange={(ev) => setSelectedPet(ev.target.value)}
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
