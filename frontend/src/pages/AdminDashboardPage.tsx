import {useEffect, useState} from "react";
import type {Stats} from "../client/dto/Stats.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";

export default function AdminDashboardPage() {
    const client = useApiClient();

    const [stats, setStats] = useState<Stats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchStats();
    }, []);

    async function fetchStats() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.getStats();
            setStats(data);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    const cardStyle: React.CSSProperties = {
        border: "1px solid #ccc",
        borderRadius: 8,
        padding: "16px 24px",
        textAlign: "center",
        minWidth: 120,
        flex: 1,
    };

    const countStyle: React.CSSProperties = {
        fontSize: "2.5rem",
        fontWeight: "bold",
        margin: "8px 0 4px",
    };

    const labelStyle: React.CSSProperties = {
        fontSize: "0.9rem",
        color: "#555",
    };

    return (
        <div style={{maxWidth: 800, margin: "0 auto", padding: 20}}>
            <h1>Admin Dashboard</h1>

            {loading && <div>Loading...</div>}
            {error && <p style={{color: "red"}}>{error}</p>}

            {!loading && !error && stats && (
                <>
                    <div style={{display: "flex", gap: 16, marginBottom: 32}}>
                        <div style={cardStyle}>
                            <div style={countStyle}>{stats.totalOwners}</div>
                            <div style={labelStyle}>Owners</div>
                        </div>
                        <div style={cardStyle}>
                            <div style={countStyle}>{stats.totalVets}</div>
                            <div style={labelStyle}>Vets</div>
                        </div>
                        <div style={cardStyle}>
                            <div style={countStyle}>{stats.totalPets}</div>
                            <div style={labelStyle}>Pets</div>
                        </div>
                        <div style={cardStyle}>
                            <div style={countStyle}>{stats.totalAppointments}</div>
                            <div style={labelStyle}>Appointments</div>
                        </div>
                    </div>

                    <h2>Appointments per Vet</h2>
                    <table style={{width: "100%", borderCollapse: "collapse"}}>
                        <thead>
                            <tr>
                                <th style={{textAlign: "left", padding: 8, borderBottom: "1px solid #ccc"}}>Vet</th>
                                <th style={{textAlign: "left", padding: 8, borderBottom: "1px solid #ccc"}}>Appointments</th>
                            </tr>
                        </thead>
                        <tbody>
                            {stats.appointmentsPerVet.length === 0 ? (
                                <tr>
                                    <td colSpan={2} style={{padding: 8, color: "#888"}}>No appointments recorded yet.</td>
                                </tr>
                            ) : (
                                stats.appointmentsPerVet.map(entry => (
                                    <tr key={entry.vetUsername} style={{borderBottom: "1px solid #eee"}}>
                                        <td style={{padding: 8}}>{entry.vetUsername}</td>
                                        <td style={{padding: 8}}>{entry.count}</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </>
            )}
        </div>
    );
}
