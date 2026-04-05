import React, { useEffect, useState } from "react";
import type {
    Location,
    OpeningOverride,
    OpeningPeriod
} from "../client/dto/Location.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { StatusMessage } from "../components/ui/StatusMessage";

const days = [
    { v: 1, label: "Mon" },
    { v: 2, label: "Tue" },
    { v: 3, label: "Wed" },
    { v: 4, label: "Thu" },
    { v: 5, label: "Fri" },
    { v: 6, label: "Sat" },
    { v: 7, label: "Sun" },
];

const input: React.CSSProperties = {
    width: "100%",
    padding: "8px 12px",
    fontSize: 14,
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-md)",
    background: "var(--color-surface)",
    color: "var(--color-text)",
    fontFamily: "inherit",
    boxSizing: "border-box",
};

const smallInput: React.CSSProperties = {
    padding: "6px 8px",
    fontSize: 13,
    width: 120,
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-md)",
    fontFamily: "inherit",
    boxSizing: "border-box",
};

export default function LocationsPage() {
    const client = useApiClient();

    const [locations, setLocations] = useState<Location[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [selected, setSelected] = useState<Location | null>(null);
    const [editing, setEditing] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        fetchLocations();
    }, []);

    async function fetchLocations() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.listLocations();
            setLocations(data || []);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    async function loadLocation(id: number) {
        setError(null);
        try {
            const data = await client.retrieveLocations(id);
            setSelected(data);
            setEditing(false);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        }
    }

    function newLocation() {
        const l: Location = {
            name: "",
            zoneId: "Europe/Vienna",
            street: "",
            postalCode: "",
            city: "",
            country: "",
            weeklyPeriods: [],
            overrides: [],
        };
        setSelected(l);
        setEditing(true);
    }

    async function deleteLocation(id?: number) {
        if (!id) return;
        if (!confirm("Delete location?")) return;
        try {
            await client.deleteLocations(id);
            setSelected(null);
            await fetchLocations();
        } catch (err: any) {
            setError(err.message || "Delete failed");
        }
    }

    function updateSelected<K extends keyof Location>(key: K, value: Location[K]) {
        if (!selected) return;
        setSelected({ ...selected, [key]: value } as Location);
    }

    // Weekly periods helpers
    function addWeeklyPeriod() {
        if (!selected) return;
        const nextSort = selected.weeklyPeriods.length;
        const p: OpeningPeriod = { dayOfWeek: 1, startTime: "09:00", endTime: "17:00", sortOrder: nextSort };
        updateSelected("weeklyPeriods", [...selected!.weeklyPeriods, p]);
    }

    function removeWeeklyPeriod(index: number) {
        if (!selected) return;
        const list = [...selected.weeklyPeriods];
        list.splice(index, 1);
        // reassign sortOrder
        const updated = list.map((p, i) => ({ ...p, sortOrder: i }));
        updateSelected("weeklyPeriods", updated);
    }

    function changeWeeklyPeriod(index: number, changes: Partial<OpeningPeriod>) {
        if (!selected) return;
        const list = [...selected.weeklyPeriods];
        list[index] = { ...list[index], ...changes };
        updateSelected("weeklyPeriods", list);
    }

    // Overrides helpers
    function addOverride() {
        if (!selected) return;
        const ov: OpeningOverride = {
            date: new Date().toISOString().slice(0, 10),
            openTime: null,
            closeTime: null,
            closed: true,
            reason: "",
        };
        updateSelected("overrides", [...selected!.overrides, ov]);
    }

    function removeOverride(index: number) {
        if (!selected) return;
        const list = [...selected.overrides];
        list.splice(index, 1);
        updateSelected("overrides", list);
    }

    function changeOverride(index: number, changes: Partial<OpeningOverride>) {
        if (!selected) return;
        const list = [...selected.overrides];
        list[index] = { ...list[index], ...changes } as OpeningOverride;
        updateSelected("overrides", list);
    }

    async function saveSelected() {
        if (!selected) return;
        setSaving(true);
        setError(null);
        try {
            const saved = await client.saveLocation(selected);
            setSelected(saved);
            setEditing(false);
            await fetchLocations();
        } catch (err: any) {
            setError(err.message || "Save failed");
        } finally {
            setSaving(false);
        }
    }

    return (
        <div style={{ maxWidth: 1100, margin: "0 auto", padding: "32px 20px" }}>
            <PageHeader title="Locations" />

            {error && (
                <div style={{ marginBottom: 16 }}>
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}

            <div style={{ display: "flex", gap: 16 }}>
                {/* Left panel — location list */}
                <div style={{ flex: "0 0 320px" }}>
                    <Card>
                        <div style={{
                            marginBottom: 12,
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center"
                        }}>
                            <strong style={{ fontSize: 14, fontWeight: 600 }}>All locations</strong>
                            <div style={{ display: "flex", gap: 6 }}>
                                <Button size="sm" variant="primary" onClick={newLocation}>New</Button>
                                <Button size="sm" variant="secondary" onClick={fetchLocations}>Refresh</Button>
                            </div>
                        </div>

                        {loading && <p style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Loading…</p>}
                        {!loading && locations.length === 0 && (
                            <p style={{ color: "var(--color-text-muted)", fontSize: 14 }}>No locations yet.</p>
                        )}

                        <ul style={{ listStyle: "none", padding: 0, maxHeight: 600, overflow: "auto", margin: 0 }}>
                            {locations.map(location => (
                                <li
                                    key={String(location.id)}
                                    style={{
                                        padding: "8px 0",
                                        borderBottom: "1px solid var(--color-border)",
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center",
                                    }}
                                >
                                    <div
                                        style={{ cursor: "pointer", fontSize: 14 }}
                                        onClick={() => loadLocation(location.id!)}
                                    >
                                        {location.name}
                                    </div>
                                    <div style={{ display: "flex", gap: 4 }}>
                                        <Button size="sm" variant="secondary" onClick={() => loadLocation(location.id!)}>Open</Button>
                                        <Button size="sm" variant="danger" onClick={() => deleteLocation(location.id)}>Del</Button>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </Card>
                </div>

                {/* Right panel — detail/edit */}
                <div style={{ flex: 1 }}>
                    <Card>
                        {!selected && (
                            <p style={{ color: "var(--color-text-muted)", fontSize: 14 }}>
                                Select a location to view/edit or click New.
                            </p>
                        )}

                        {selected && (
                            <>
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
                                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600 }}>
                                        {selected.id ? `#${selected.id} ${selected.name}` : "New location"}
                                    </h2>
                                    <div style={{ display: "flex", gap: 6 }}>
                                        {!editing && <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>Edit</Button>}
                                        {editing && (
                                            <Button size="sm" variant="secondary" onClick={() => {
                                                setSelected(null);
                                                setEditing(false);
                                            }}>Cancel</Button>
                                        )}
                                        <Button size="sm" variant="danger" onClick={() => selected && deleteLocation(selected.id)}>Delete</Button>
                                    </div>
                                </div>

                                {/* Address fields */}
                                {[
                                    { key: "name" as keyof Location, label: "Name" },
                                    { key: "zoneId" as keyof Location, label: "Zone ID" },
                                    { key: "street" as keyof Location, label: "Street" },
                                    { key: "postalCode" as keyof Location, label: "Postal Code" },
                                    { key: "city" as keyof Location, label: "City" },
                                    { key: "country" as keyof Location, label: "Country" },
                                ].map(({ key, label }) => (
                                    <div key={key} style={{ marginBottom: 12 }}>
                                        <label style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-muted)" }}>{label}</label>
                                        <input
                                            style={input}
                                            value={(selected[key] as string) ?? ""}
                                            onChange={e => editing ? updateSelected(key, e.target.value as Location[typeof key]) : undefined}
                                            disabled={!editing}
                                        />
                                    </div>
                                ))}

                                {/* Weekly Periods */}
                                <div style={{ marginBottom: 12 }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                                        <strong style={{ fontSize: 14 }}>Weekly periods</strong>
                                        {editing && <Button size="sm" variant="secondary" onClick={addWeeklyPeriod}>Add period</Button>}
                                    </div>

                                    {selected.weeklyPeriods.length === 0 && (
                                        <p style={{ color: "var(--color-text-muted)", fontSize: 13 }}>No weekly periods</p>
                                    )}

                                    <div>
                                        {selected.weeklyPeriods.map((p, idx) => (
                                            <div key={idx} style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 6 }}>
                                                <select
                                                    value={p.dayOfWeek}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, { dayOfWeek: Number(e.target.value) })}
                                                    disabled={!editing}
                                                    style={{ padding: "6px 8px", borderRadius: "var(--radius-md)", border: "1px solid var(--color-border-strong)", fontFamily: "inherit" }}
                                                >
                                                    {days.map(d => <option value={d.v} key={d.v}>{d.label}</option>)}
                                                </select>

                                                <input
                                                    type="time"
                                                    value={p.startTime}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, { startTime: e.target.value })}
                                                    style={smallInput}
                                                    disabled={!editing}
                                                />
                                                <span style={{ color: "var(--color-text-muted)" }}>-</span>
                                                <input
                                                    type="time"
                                                    value={p.endTime}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, { endTime: e.target.value })}
                                                    style={smallInput}
                                                    disabled={!editing}
                                                />

                                                <Button size="sm" variant="danger" onClick={() => removeWeeklyPeriod(idx)} disabled={!editing}>
                                                    Remove
                                                </Button>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Overrides */}
                                <div style={{ marginBottom: 12 }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                                        <strong style={{ fontSize: 14 }}>Overrides (holidays/special days)</strong>
                                        {editing && <Button size="sm" variant="secondary" onClick={addOverride}>Add override</Button>}
                                    </div>

                                    {selected.overrides.length === 0 && (
                                        <p style={{ color: "var(--color-text-muted)", fontSize: 13 }}>No overrides</p>
                                    )}

                                    <div>
                                        {selected.overrides.map((ov, i) => (
                                            <div
                                                key={i}
                                                style={{
                                                    border: "1px dashed var(--color-border)",
                                                    padding: 8,
                                                    marginBottom: 8,
                                                    borderRadius: "var(--radius-md)",
                                                }}
                                            >
                                                <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                                                    <input
                                                        type="date"
                                                        value={ov.date}
                                                        onChange={e => editing && changeOverride(i, { date: e.target.value })}
                                                        disabled={!editing}
                                                        style={{ padding: "6px 8px", borderRadius: "var(--radius-md)", border: "1px solid var(--color-border-strong)", fontFamily: "inherit" }}
                                                    />
                                                    <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 14 }}>
                                                        <input
                                                            type="checkbox"
                                                            checked={ov.closed}
                                                            onChange={e => editing && changeOverride(i, { closed: e.target.checked })}
                                                            disabled={!editing}
                                                        />
                                                        Closed
                                                    </label>

                                                    <input
                                                        placeholder="reason"
                                                        value={ov.reason ?? ""}
                                                        onChange={e => editing && changeOverride(i, { reason: e.target.value })}
                                                        disabled={!editing}
                                                        style={{ ...input, width: 220 }}
                                                    />

                                                    {editing && (
                                                        <Button size="sm" variant="danger" onClick={() => removeOverride(i)}>Remove</Button>
                                                    )}
                                                </div>

                                                {!ov.closed && (
                                                    <div style={{ display: "flex", gap: 8, alignItems: "center", marginTop: 6 }}>
                                                        <input
                                                            type="time"
                                                            value={ov.openTime ?? ""}
                                                            onChange={e => editing && changeOverride(i, { openTime: e.target.value })}
                                                            disabled={!editing}
                                                            style={smallInput}
                                                        />
                                                        <span style={{ color: "var(--color-text-muted)" }}>-</span>
                                                        <input
                                                            type="time"
                                                            value={ov.closeTime ?? ""}
                                                            onChange={e => editing && changeOverride(i, { closeTime: e.target.value })}
                                                            disabled={!editing}
                                                            style={smallInput}
                                                        />
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Actions */}
                                <div style={{ display: "flex", gap: 8 }}>
                                    {editing ? (
                                        <>
                                            <Button variant="primary" onClick={saveSelected} loading={saving}>
                                                {saving ? "Saving..." : "Save"}
                                            </Button>
                                            <Button variant="secondary" onClick={() => {
                                                setEditing(false);
                                                loadLocation(selected.id!);
                                            }}>
                                                Cancel
                                            </Button>
                                        </>
                                    ) : (
                                        <Button variant="secondary" onClick={() => setEditing(true)}>Edit</Button>
                                    )}

                                    <Button variant="secondary" onClick={() => { setSelected(null); }}>Close</Button>
                                </div>
                            </>
                        )}
                    </Card>
                </div>
            </div>
        </div>
    );
}
