import React, {useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useApiClient} from "../hooks/useApiClient.ts";
import type {OwnerVisit} from "../client/dto/OwnerVisit.tsx";

export default function PetVisitsPage() {
    const {petId} = useParams<{petId: string}>();
    const client = useApiClient();
    const navigate = useNavigate();

    const [visits, setVisits] = useState<OwnerVisit[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function loadVisits() {
            setLoading(true);
            setError(null);
            try {
                const data = await client.listPetVisits(Number(petId));
                setVisits(data);
            } catch (err: any) {
                setError(err.message || "Failed to load visits");
            } finally {
                setLoading(false);
            }
        }
        loadVisits();
    }, [petId]);

    const btn: React.CSSProperties = {padding: "6px 14px", cursor: "pointer"};
    const row: React.CSSProperties = {padding: 10, borderBottom: "1px solid #eee"};

    return (
        <div style={{maxWidth: 800, margin: "0 auto", padding: 20}}>
            <h1>Pet Visits</h1>

            {loading && <div>Loading...</div>}

            {error && <p style={{color: "red"}}>{error}</p>}

            {!loading && !error && visits.length === 0 && (
                <div>No visits found.</div>
            )}

            {!loading && !error && visits.length > 0 && (
                <ul style={{listStyle: "none", padding: 0}}>
                    {visits.map(v => (
                        <li key={v.id} style={row}>
                            <strong>Vet:</strong> <span>{v.vetUsername}</span><br/>
                            <strong>Date:</strong> <span>{new Date(v.startsAt).toLocaleString()}</span><br/>
                            <strong>Owner Summary:</strong> <span>{v.ownerSummary ?? "—"}</span><br/>
                            <strong>Vaccination:</strong> <span>{v.vaccination ?? "—"}</span>
                        </li>
                    ))}
                </ul>
            )}

            <button style={btn} onClick={() => navigate("/pets")}>Back</button>
        </div>
    );
}
