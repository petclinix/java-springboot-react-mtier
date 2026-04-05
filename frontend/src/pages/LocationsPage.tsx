import React, {useEffect, useState} from "react";
import type {
    Location,
    OpeningOverride,
    OpeningPeriod
} from "../client/dto/Location.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";

const days = [
    {v: 1, label: "Mon"},
    {v: 2, label: "Tue"},
    {v: 3, label: "Wed"},
    {v: 4, label: "Thu"},
    {v: 5, label: "Fri"},
    {v: 6, label: "Sat"},
    {v: 7, label: "Sun"},
];

const container: React.CSSProperties = {maxWidth: 1100, margin: "0 auto", padding: 16, fontFamily: "sans-serif"};
const panel: React.CSSProperties = {border: "1px solid #ddd", padding: 12, borderRadius: 6, background: "#fff"};
const input: React.CSSProperties = {width: "100%", padding: 6, marginTop: 4, boxSizing: "border-box"};
const smallInput: React.CSSProperties = {padding: 6, width: 120, boxSizing: "border-box"};
const btn: React.CSSProperties = {padding: "6px 10px", cursor: "pointer", marginRight: 8};

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
        setSelected({...selected, [key]: value} as Location);
    }

    // Weekly periods helpers
    function addWeeklyPeriod() {
        if (!selected) return;
        const nextSort = selected.weeklyPeriods.length;
        const p: OpeningPeriod = {dayOfWeek: 1, startTime: "09:00", endTime: "17:00", sortOrder: nextSort};
        updateSelected("weeklyPeriods", [...selected!.weeklyPeriods, p]);
    }

    function removeWeeklyPeriod(index: number) {
        if (!selected) return;
        const list = [...selected.weeklyPeriods];
        list.splice(index, 1);
        // reassign sortOrder
        const updated = list.map((p, i) => ({...p, sortOrder: i}));
        updateSelected("weeklyPeriods", updated);
    }

    function changeWeeklyPeriod(index: number, changes: Partial<OpeningPeriod>) {
        if (!selected) return;
        const list = [...selected.weeklyPeriods];
        list[index] = {...list[index], ...changes};
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
        list[index] = {...list[index], ...changes} as OpeningOverride;
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
        <div style={container}>
            <h1>Locations</h1>
            <div style={{display: "flex", gap: 16}}>
                <div style={{flex: "0 0 320px"}}>
                    <div style={{...panel}}>
                        <div style={{
                            marginBottom: 8,
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center"
                        }}>
                            <strong>All locations</strong>
                            <div>
                                <button style={btn} onClick={newLocation}>New</button>
                                <button style={btn} onClick={fetchLocations}>Refresh</button>
                            </div>
                        </div>

                        {loading && <div>Loading...</div>}
                        {!loading && locations.length === 0 && <div>No locations yet.</div>}

                        <ul style={{listStyle: "none", padding: 0, maxHeight: 600, overflow: "auto"}}>
                            {locations.map(location => (
                                <li key={String(location.id)} style={{
                                    padding: 8,
                                    borderBottom: "1px solid #eee",
                                    display: "flex",
                                    justifyContent: "space-between"
                                }}>
                                    <div style={{cursor: "pointer"}} onClick={() => loadLocation(location.id!)}>
                                        <div style={{fontSize: 12, color: "#666"}}>{location.name}</div>
                                    </div>
                                    <div style={{display: "flex", alignItems: "center", gap: 6}}>
                                        <button style={btn} onClick={() => loadLocation(location.id!)}>Open</button>
                                        <button style={btn} onClick={() => deleteLocation(location.id)}>Del</button>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>

                <div style={{flex: 1}}>
                    <div style={panel}>
                        {!selected && <div>Select a location to view/edit or click New.</div>}

                        {selected && (
                            <>
                                <div style={{display: "flex", justifyContent: "space-between", alignItems: "center"}}>
                                    <h2>{selected.id ? `#${selected.id} ${selected.name}` : "New location"}</h2>
                                    <div>
                                        {!editing && <button style={btn} onClick={() => setEditing(true)}>Edit</button>}
                                        {editing && <button style={btn} onClick={() => {
                                            setSelected(null);
                                            setEditing(false);
                                        }}>Cancel</button>}
                                        <button style={btn}
                                                onClick={() => selected && deleteLocation(selected.id)}>Delete
                                        </button>
                                    </div>
                                </div>

                                <div style={{marginBottom: 12}}>
                                    <label style={{fontSize: 12}}>Name</label>
                                    <input
                                        style={input}
                                        value={selected.name}
                                        onChange={e => editing ? updateSelected("name", e.target.value) : null}
                                        disabled={!editing}
                                    />
                                </div>

                                <div style={{marginBottom: 12}}>
                                    <label style={{fontSize: 12}}>Zone ID</label>
                                    <input
                                        style={input}
                                        value={selected.zoneId}
                                        onChange={e => editing ? updateSelected("zoneId", e.target.value) : null}
                                        disabled={!editing}
                                    />
                                </div>

                                <div style={{marginBottom: 12}}>
                                    <label style={{fontSize: 12}}>Street</label>
                                    <input
                                        style={input}
                                        value={selected.street ?? ""}
                                        onChange={e => editing ? updateSelected("street", e.target.value) : null}
                                        disabled={!editing}
                                    />
                                </div>

                                <div style={{marginBottom: 12}}>
                                    <label style={{fontSize: 12}}>Postal Code</label>
                                    <input
                                        style={input}
                                        value={selected.postalCode ?? ""}
                                        onChange={e => editing ? updateSelected("postalCode", e.target.value) : null}
                                        disabled={!editing}
                                    />
                                </div>

                                <div style={{marginBottom: 12}}>
                                    <label style={{fontSize: 12}}>City</label>
                                    <input
                                        style={input}
                                        value={selected.city ?? ""}
                                        onChange={e => editing ? updateSelected("city", e.target.value) : null}
                                        disabled={!editing}
                                    />
                                </div>

                                <div style={{marginBottom: 12}}>
                                    <label style={{fontSize: 12}}>Country</label>
                                    <input
                                        style={input}
                                        value={selected.country ?? ""}
                                        onChange={e => editing ? updateSelected("country", e.target.value) : null}
                                        disabled={!editing}
                                    />
                                </div>

                                {/* Weekly Periods */}
                                <div style={{marginBottom: 12}}>
                                    <div style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center"
                                    }}>
                                        <strong>Weekly periods</strong>
                                        {editing && <button style={btn} onClick={addWeeklyPeriod}>Add period</button>}
                                    </div>

                                    {selected.weeklyPeriods.length === 0 &&
                                        <div style={{color: "#666", marginTop: 8}}>No weekly periods</div>}

                                    <div style={{marginTop: 8}}>
                                        {selected.weeklyPeriods.map((p, idx) => (
                                            <div key={idx} style={{
                                                display: "flex",
                                                gap: 8,
                                                alignItems: "center",
                                                marginBottom: 6
                                            }}>
                                                <select
                                                    value={p.dayOfWeek}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, {dayOfWeek: Number(e.target.value)})}
                                                    disabled={!editing}
                                                >
                                                    {days.map(d => <option value={d.v} key={d.v}>{d.label}</option>)}
                                                </select>

                                                <input
                                                    type="time"
                                                    value={p.startTime}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, {startTime: e.target.value})}
                                                    style={smallInput}
                                                    disabled={!editing}
                                                />
                                                <span>-</span>
                                                <input
                                                    type="time"
                                                    value={p.endTime}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, {endTime: e.target.value})}
                                                    style={smallInput}
                                                    disabled={!editing}
                                                />

                                                <button style={btn} onClick={() => removeWeeklyPeriod(idx)}
                                                        disabled={!editing}>Remove
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Overrides */}
                                <div style={{marginBottom: 12}}>
                                    <div style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center"
                                    }}>
                                        <strong>Overrides (holidays/special days)</strong>
                                        {editing && <button style={btn} onClick={addOverride}>Add override</button>}
                                    </div>

                                    {selected.overrides.length === 0 &&
                                        <div style={{color: "#666", marginTop: 8}}>No overrides</div>}

                                    <div style={{marginTop: 8}}>
                                        {selected.overrides.map((ov, i) => (
                                            <div key={i}
                                                 style={{border: "1px dashed #eee", padding: 8, marginBottom: 8}}>
                                                <div style={{display: "flex", gap: 8, alignItems: "center"}}>
                                                    <input
                                                        type="date"
                                                        value={ov.date}
                                                        onChange={e => editing && changeOverride(i, {date: e.target.value})}
                                                        disabled={!editing}
                                                    />
                                                    <label style={{display: "flex", alignItems: "center", gap: 6}}>
                                                        <input
                                                            type="checkbox"
                                                            checked={ov.closed}
                                                            onChange={e => editing && changeOverride(i, {closed: e.target.checked})}
                                                            disabled={!editing}
                                                        />
                                                        Closed
                                                    </label>

                                                    <input
                                                        placeholder="reason"
                                                        value={ov.reason ?? ""}
                                                        onChange={e => editing && changeOverride(i, {reason: e.target.value})}
                                                        disabled={!editing}
                                                        style={{...input, width: 220}}
                                                    />

                                                    {editing && <button style={btn}
                                                                        onClick={() => removeOverride(i)}>Remove</button>}
                                                </div>

                                                {!ov.closed && (
                                                    <div style={{display: "flex", gap: 8, alignItems: "center", marginTop: 6}}>
                                                        <input
                                                            type="time"
                                                            value={ov.openTime ?? ""}
                                                            onChange={e => editing && changeOverride(i, {openTime: e.target.value})}
                                                            disabled={!editing}
                                                        />
                                                        <span>-</span>
                                                        <input
                                                            type="time"
                                                            value={ov.closeTime ?? ""}
                                                            onChange={e => editing && changeOverride(i, {closeTime: e.target.value})}
                                                            disabled={!editing}
                                                        />
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* actions */}
                                <div style={{display: "flex", gap: 8}}>
                                    {editing ? (
                                        <>
                                            <button style={btn} onClick={saveSelected}
                                                    disabled={saving}>{saving ? "Saving..." : "Save"}</button>
                                            <button style={btn} onClick={() => {
                                                setEditing(false);
                                                loadLocation(selected.id!);
                                            }}>Discard
                                            </button>
                                        </>
                                    ) : (
                                        <button style={btn} onClick={() => setEditing(true)}>Edit</button>
                                    )}

                                    <button style={btn} onClick={() => {
                                        setSelected(null);
                                    }}>Close
                                    </button>
                                </div>

                                {error && <div style={{color: "red", marginTop: 8}}>{error}</div>}
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
