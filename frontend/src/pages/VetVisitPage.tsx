import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function VetVisitPage() {
    const { appointmentId } = useParams<{ appointmentId: string }>();
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
            await client.saveVetVisit(Number(appointmentId), { vetSummary, vaccination, ownerSummary });
            setSaveSuccess(true);
        } catch (err: any) {
            setSaveError(err.message || "Failed to save visit");
        }
    }

    return (
        <PageLayout narrow>
            <PageHeader
                title="Visit Documentation"
                actions={
                    <Button variant="secondary" onClick={() => navigate("/appointments/vet")}>Back</Button>
                }
            />

            {loading && <p className="text-muted">Loading...</p>}

            {fetchError && <StatusMessage variant="error">{fetchError}</StatusMessage>}

            {!loading && !fetchError && (
                <Card>
                    <div className="flex flex-col gap-[16px]">
                        <div className="flex flex-col gap-[4px]">
                            <label htmlFor="vetSummary" className="text-[13px] font-semibold text-[#1e293b]">Vet Summary</label>
                            <textarea
                                id="vetSummary"
                                value={vetSummary}
                                onChange={e => setVetSummary(e.target.value)}
                                rows={5}
                                className="w-full px-[12px] py-[8px] text-[14px] border border-border-strong rounded-card font-[inherit] resize-y"
                            />
                        </div>

                        <div className="flex flex-col gap-[4px]">
                            <label htmlFor="vaccination" className="text-[13px] font-semibold text-[#1e293b]">Vaccination</label>
                            <input
                                id="vaccination"
                                type="text"
                                value={vaccination}
                                onChange={e => setVaccination(e.target.value)}
                                className="w-full px-[12px] py-[8px] text-[14px] border border-border-strong rounded-card font-[inherit]"
                            />
                        </div>

                        <div className="flex flex-col gap-[4px]">
                            <label htmlFor="ownerSummary" className="text-[13px] font-semibold text-[#1e293b]">Owner Summary</label>
                            <textarea
                                id="ownerSummary"
                                value={ownerSummary}
                                onChange={e => setOwnerSummary(e.target.value)}
                                rows={5}
                                className="w-full px-[12px] py-[8px] text-[14px] border border-border-strong rounded-card font-[inherit] resize-y"
                            />
                        </div>

                        {saveSuccess && <StatusMessage variant="success">Saved successfully.</StatusMessage>}
                        {saveError && <StatusMessage variant="error">{saveError}</StatusMessage>}

                        <div className="flex gap-[8px]">
                            <Button variant="primary" onClick={handleSave}>Save</Button>
                        </div>
                    </div>
                </Card>
            )}
        </PageLayout>
    );
}
