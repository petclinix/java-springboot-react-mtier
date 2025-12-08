import React, {useEffect, useMemo, useState} from "react";
import {useAuth} from "../context/AuthContext.tsx";
import ApiClient from "../client/ApiClient.tsx";
import type {Pet} from "../client/dto/Pet.tsx";
const DEFAULT_SPECIES = ["DOG", "CAT", "BIRD", "RABBIT", "REPTILE", "OTHER"];
const DEFAULT_GENDERS = ["MALE", "FEMALE", "UNKNOWN"];

export default function PetsPage() {
    const {token} = useAuth();
    const client = useMemo(() => new ApiClient(() => token), [token]);

    const [pets, setPets] = useState<Pet[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [form, setForm] = useState<Pet>({name: "", species: "DOG", gender: "UNKNOWN", birthDate: ""});
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
        setForm(prev => ({...prev, [key]: value}));
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
            setForm({name: "", species: "DOG", gender: "UNKNOWN", birthDate: ""});
        } catch (err: any) {
            setError(err.message || "Failed to create pet");
        } finally {
            setSubmitting(false);
        }
    }

    const box: React.CSSProperties = {
        border: "1px solid #ccc",
        borderRadius: 6,
        padding: 12,
        marginBottom: 16,
        //background: "white"
    };

    const inputStyle: React.CSSProperties = {
        width: "100%",
        padding: "6px 8px",
        marginTop: 4
    };

    const button: React.CSSProperties = {
        padding: "6px 12px",
        marginRight: 8,
        cursor: "pointer"
    };

    return (
        <div style={{maxWidth: 900, margin: "0 auto", padding: 20}}>
            <h1>Pets</h1>

            {/* Add Pet Form */}
            <div style={box}>
                <h2>Add Pet</h2>
                <form onSubmit={handleSubmit}>
                    <div>
                        <label>Name</label>
                        <input
                            style={inputStyle}
                            value={form.name}
                            onChange={e => handleChange("name", e.target.value)}
                            required
                        />
                    </div>

                    <div>
                        <label>Species</label>
                        <select
                            style={inputStyle}
                            value={form.species}
                            onChange={e => handleChange("species", e.target.value)}
                        >
                            {DEFAULT_SPECIES.map(s => (
                                <option key={s} value={s}>{s}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label>Gender</label>
                        <select
                            style={inputStyle}
                            value={form.gender}
                            onChange={e => handleChange("gender", e.target.value)}
                        >
                            {DEFAULT_GENDERS.map(g => (
                                <option key={g} value={g}>{g}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label>Birth date</label>
                        <input
                            type="date"
                            style={inputStyle}
                            value={form.birthDate || ""}
                            onChange={e => handleChange("birthDate", e.target.value)}
                        />
                    </div>

                    {error && <p style={{color: "red"}}>{error}</p>}

                    <button type="submit" style={button} disabled={submitting}>
                        {submitting ? "Adding..." : "Add Pet"}
                    </button>
                    <button type="button" style={button} onClick={fetchPets}>Refresh</button>
                </form>
            </div>

            {/* List */}
            <div style={box}>
                <h2>All Pets</h2>
                {loading && <div>Loading...</div>}
                {!loading && pets.length === 0 && <div>No pets found.</div>}

                <ul style={{listStyle: "none", padding: 0}}>
                    {pets.map(p => (
                        <li key={p.id} style={{padding: 10, borderBottom: "1px solid #eee"}}>
                            <strong>{p.name}</strong> — {p.species}<br/>
                            {p.gender} {p.birthDate ? `· ${p.birthDate}` : ""}<br/>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
