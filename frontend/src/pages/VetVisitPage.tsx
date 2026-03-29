import {useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useApiClient} from "../hooks/useApiClient.ts";

export default function VetVisitPage() {
    const {appointmentId} = useParams<{appointmentId: string}>();
    const client = useApiClient();
    const navigate = useNavigate();

    const [vetSummary, setVetSummary] = useState("");
    const [vaccination, setVaccination] = useState("");
    const [ownerSummary, setOwnerSummary] = useState("");
    const [loading, setLoading] = useState(true);
    const [fetchError, setFetchError] = useState<string | null>(null);
    const [saveSuccess, setSaveSuccess] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    useEffect(() => {
        async function loadVisit() {
            setLoading(true);
            setFetchError(null);
            try {
                const visit = await client.getVetVisit(Number(appointmentId));
                setVetSummary(visit.vetSummary ?? "");
                setVaccination(visit.vaccination ?? "");
                setOwnerSummary(visit.ownerSummary ?? "");
            } catch (err: any) {
                setFetchError(err.message || "Failed to load visit");
            } finally {
                setLoading(false);
            }
        }
        loadVisit();
    }, [appointmentId]);

    async function handleSave() {
        setSaveSuccess(false);
        setSaveError(null);
        try {
            await client.saveVetVisit(Number(appointmentId), {vetSummary, vaccination, ownerSummary});
            setSaveSuccess(true);
        } catch (err: any) {
            setSaveError(err.message || "Failed to save visit");
        }
    }

    const fieldStyle = {display: "flex", flexDirection: "column" as const, gap: 4, marginBottom: 16};
    const btn: React.CSSProperties = {padding: "6px 14px", cursor: "pointer"};

    return (
        <div style={{maxWidth: 600, margin: "0 auto", padding: 20}}>
            <h1>Visit Documentation</h1>

            {loading && <div>Loading...</div>}

            {fetchError && <p style={{color: "red"}}>{fetchError}</p>}

            {!loading && !fetchError && (
                <>
                    <div style={fieldStyle}>
                        <label htmlFor="vetSummary">Vet Summary</label>
                        <textarea
                            id="vetSummary"
                            value={vetSummary}
                            onChange={e => setVetSummary(e.target.value)}
                            rows={5}
                            style={{padding: 8, fontSize: 14}}
                        />
                    </div>

                    <div style={fieldStyle}>
                        <label htmlFor="vaccination">Vaccination</label>
                        <input
                            id="vaccination"
                            type="text"
                            value={vaccination}
                            onChange={e => setVaccination(e.target.value)}
                            style={{padding: 8, fontSize: 14}}
                        />
                    </div>

                    <div style={fieldStyle}>
                        <label htmlFor="ownerSummary">Owner Summary</label>
                        <textarea
                            id="ownerSummary"
                            value={ownerSummary}
                            onChange={e => setOwnerSummary(e.target.value)}
                            rows={5}
                            style={{padding: 8, fontSize: 14}}
                        />
                    </div>

                    {saveSuccess && <p style={{color: "green"}}>Saved successfully.</p>}
                    {saveError && <p style={{color: "red"}}>{saveError}</p>}

                    <div style={{display: "flex", gap: 8}}>
                        <button style={btn} onClick={handleSave}>Save</button>
                        <button style={btn} onClick={() => navigate("/appointments/vet")}>Back</button>
                    </div>
                </>
            )}
        </div>
    );
}
