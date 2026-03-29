import React, {useEffect, useState} from "react";
import type {VetAppointment} from "../client/dto/VetAppointment.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";

export default function VetAppointmentsPage() {
    const client = useApiClient();

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

    const box: React.CSSProperties = {
        border: "1px solid #ccc",
        borderRadius: 6,
        padding: 12,
        marginBottom: 16,
    };

    const btn: React.CSSProperties = {padding: "4px 10px", cursor: "pointer"};

    return (
        <div style={{maxWidth: 900, margin: "0 auto", padding: 20}}>
            <div style={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16}}>
                <h1 style={{margin: 0}}>My Appointments</h1>
                <button style={btn} onClick={fetchAppointments}>Refresh</button>
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
                                Pet: {a.petName} · Owner: {a.ownerUsername}
                            </div>
                            <button
                                style={{...btn, color: "red"}}
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
