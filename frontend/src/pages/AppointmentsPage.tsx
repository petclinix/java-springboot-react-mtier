import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Appointment } from "../client/dto/Appointment.tsx";
import type { Pet } from "../client/dto/Pet.tsx";
import type { Vet } from "../client/dto/Vet.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { StatusMessage } from "../components/ui/StatusMessage";

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
        return vets.find(v => v.id === vetId)?.username ?? `Vet #${vetId}`;
    }

    return (
        <PageLayout>
            <PageHeader
                title="My Appointments"
                actions={
                    <Link to="/appointments/book">
                        <Button variant="primary">+ Book appointment</Button>
                    </Link>
                }
            />

            {error && (
                <div className="mb-[16px]">
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}

            <Card>
                {loading && <p className="text-muted">Loading...</p>}
                {!loading && appointments.length === 0 && (
                    <EmptyState
                        message="No appointments found."
                        action={
                            <Link to="/appointments/book">
                                <Button variant="primary">Book your first appointment</Button>
                            </Link>
                        }
                    />
                )}
                <ul className="list-none p-0 m-0">
                    {appointments.map(a => (
                        <li
                            key={a.id}
                            className="flex justify-between items-center py-[12px] border-b border-default"
                        >
                            <div>
                                <strong className="text-[15px]">{new Date(a.startsAt).toLocaleString()}</strong>
                                <p className="mt-[4px] mb-0 text-[13px] text-muted">
                                    Pet: {petName(a.petId)} · Vet: {vetName(a.vetId)}
                                </p>
                            </div>
                            <Button
                                variant="danger"
                                size="sm"
                                disabled={cancelling === a.id}
                                onClick={() => handleCancel(a.id)}
                            >
                                {cancelling === a.id ? "Cancelling…" : "Cancel"}
                            </Button>
                        </li>
                    ))}
                </ul>
            </Card>
        </PageLayout>
    );
}
