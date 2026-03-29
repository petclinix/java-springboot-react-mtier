import React, {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import type {Appointment} from "../client/dto/Appointment.tsx";
import type {Pet} from "../client/dto/Pet.tsx";
import type {Vet} from "../client/dto/Vet.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";

export default function AppointmentsPage() {
    const client = useApiClient();

    const [appointments, setAppointments] = useState<Appointment[]>([]);
    const [pets, setPets] = useState<Pet[]>([]);
    const [vets, setVets] = useState<Vet[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [cancelling, setCancelling] = useState<number | null>(null);

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

    function petName(petId: number): string {
        return pets.find(p => p.id === petId)?.name ?? `Pet #${petId}`;
    }

    function vetName(vetId: number): string {
        return vets.find(v => v.id === vetId)?.name ?? `Vet #${vetId}`;
    }

    const box: React.CSSProperties = {
        border: "1px solid #ccc",
        borderRadius: 6,
        padding: 12,
        marginBottom: 16,
    };

    const button: React.CSSProperties = {
        padding: "4px 10px",
        cursor: "pointer",
    };

    return (
        <div style={{maxWidth: 900, margin: "0 auto", padding: 20}}>
            <div style={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16}}>
                <h1 style={{margin: 0}}>My Appointments</h1>
                <Link to="/appointments/book">
                    <button style={button}>+ Book appointment</button>
                </Link>
            </div>

            {error && <p style={{color: "red"}}>{error}</p>}

            <div style={box}>
                {loading && <div>Loading...</div>}
                {!loading && appointments.length === 0 && <div>No appointments found.</div>}

                <ul style={{listStyle: "none", padding: 0, margin: 0}}>
                    {appointments.map(a => (
                        <li key={a.id} style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            padding: "10px 0",
                            borderBottom: "1px solid #eee",
                        }}>
                            <div>
                                <strong>{new Date(a.startsAt).toLocaleString()}</strong><br/>
                                Pet: {petName(a.petId)} · Vet: {vetName(a.vetId)}
                            </div>
                            <button
                                style={{...button, color: "red"}}
                                disabled={cancelling === a.id}
                                onClick={() => handleCancel(a.id)}
                            >
                                {cancelling === a.id ? "Cancelling…" : "Cancel"}
                            </button>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}