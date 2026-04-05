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

            <Card style={{ marginBottom: 24 }}>
                <h2 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 600 }}>Add Pet</h2>
                <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
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
                <h2 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 600 }}>All Pets</h2>
                {loading && <p style={{ color: "var(--color-text-muted)" }}>Loading…</p>}
                {!loading && pets.length === 0 && (
                    <EmptyState message="No pets found." />
                )}
                <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                    {pets.map(p => (
                        <li
                            key={p.id}
                            style={{
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                                padding: "12px 0",
                                borderBottom: "1px solid var(--color-border)",
                            }}
                        >
                            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                                <strong style={{ fontSize: 15 }}>{p.name}</strong>
                                <div style={{ display: "flex", gap: 6 }}>
                                    <Badge variant="neutral">{p.species}</Badge>
                                    {p.gender && <Badge variant="neutral">{p.gender}</Badge>}
                                    {p.birthDate && (
                                        <span style={{ fontSize: 12, color: "var(--color-text-muted)" }}>{p.birthDate}</span>
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
