import React, { useEffect, useState } from "react";
import type { Pet } from "../client/dto/Pet.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { useNavigate } from "react-router-dom";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { FormField } from "../components/ui/FormField";
import { Input } from "../components/ui/Input";
import { Select } from "../components/ui/Select";
import { Button } from "../components/ui/Button";
import { Badge } from "../components/ui/Badge";
import { EmptyState } from "../components/ui/EmptyState";
import { StatusMessage } from "../components/ui/StatusMessage";

const DEFAULT_SPECIES = ["DOG", "CAT", "BIRD", "RABBIT", "REPTILE", "OTHER"];
const DEFAULT_GENDERS = ["MALE", "FEMALE", "UNKNOWN"];

export default function PetsPage() {
    const client = useApiClient();
    const navigate = useNavigate();

    const [pets, setPets] = useState<Pet[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [form, setForm] = useState<Pet>({ name: "", species: "DOG", gender: "UNKNOWN", birthDate: "" });
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        fetchPets();
    }, []);

    async function fetchPets() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.listPets();
            setPets(data);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    function handleChange<K extends keyof Pet>(key: K, value: Pet[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setSubmitting(true);
        setError(null);

        if (!form.name || !form.species) {
            setError("Please provide at least a name and species.");
            setSubmitting(false);
            return;
        }

        try {
            const created = await client.createPet({
                name: form.name!,
                species: form.species!,
                gender: form.gender!,
                birthDate: form.birthDate || null,
            });
            setPets((prev) => [created, ...prev]);
            setForm({ name: "", species: "DOG", gender: "UNKNOWN", birthDate: "" });
        } catch (err: any) {
            setError(err.message || "Failed to create pet");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <PageLayout>
            <PageHeader
                title="Pets"
                actions={
                    <Button variant="secondary" onClick={fetchPets}>Refresh</Button>
                }
            />

            <Card className="mb-[24px]">
                <h2 className="m-0 mb-[16px] text-[18px] font-semibold">Add Pet</h2>
                <form onSubmit={handleSubmit} className="flex flex-col gap-[12px]">
                    <FormField label="Name">
                        <Input
                            value={form.name}
                            onChange={e => handleChange("name", e.target.value)}
                            required
                        />
                    </FormField>

                    <FormField label="Species">
                        <Select
                            value={form.species}
                            onChange={e => handleChange("species", e.target.value)}
                        >
                            {DEFAULT_SPECIES.map(s => (
                                <option key={s} value={s}>{s}</option>
                            ))}
                        </Select>
                    </FormField>

                    <FormField label="Gender">
                        <Select
                            value={form.gender}
                            onChange={e => handleChange("gender", e.target.value)}
                        >
                            {DEFAULT_GENDERS.map(g => (
                                <option key={g} value={g}>{g}</option>
                            ))}
                        </Select>
                    </FormField>

                    <FormField label="Birth date">
                        <Input
                            type="date"
                            value={form.birthDate || ""}
                            onChange={e => handleChange("birthDate", e.target.value)}
                        />
                    </FormField>

                    {error && <StatusMessage variant="error">{error}</StatusMessage>}

                    <div>
                        <Button type="submit" variant="primary" loading={submitting}>
                            {submitting ? "Adding..." : "Add Pet"}
                        </Button>
                    </div>
                </form>
            </Card>

            <Card>
                <h2 className="m-0 mb-[16px] text-[18px] font-semibold">All Pets</h2>
                {loading && <p className="text-muted">Loading…</p>}
                {!loading && pets.length === 0 && (
                    <EmptyState message="No pets found." />
                )}
                <ul className="list-none p-0 m-0">
                    {pets.map(p => (
                        <li
                            key={p.id}
                            className="flex justify-between items-center py-[12px] border-b border-border"
                        >
                            <div className="flex flex-col gap-[4px]">
                                <strong className="text-[15px]">{p.name}</strong>
                                <div className="flex gap-[6px]">
                                    <Badge variant="neutral">{p.species}</Badge>
                                    {p.gender && <Badge variant="neutral">{p.gender}</Badge>}
                                    {p.birthDate && (
                                        <span className="text-[12px] text-muted">{p.birthDate}</span>
                                    )}
                                </div>
                            </div>
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => navigate(`/pets/${p.id}/visits`)}
                            >
                                View Visits
                            </Button>
                        </li>
                    ))}
                </ul>
            </Card>
        </PageLayout>
    );
}
