import React, {useEffect, useState} from "react";
import type {
    Location,
    OpeningException,
    OpeningExceptionPeriod,
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
            weeklyPeriods: [],
            exceptions: [],
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

    // Exceptions helpers
    function addException() {
        if (!selected) return;
        const ex: OpeningException = {
            date: new Date().toISOString().slice(0, 10),
            closed: true,
            note: "",
            periods: []
        };
        updateSelected("exceptions", [...selected!.exceptions, ex]);
    }

    function removeException(index: number) {
        if (!selected) return;
        const list = [...selected.exceptions];
        list.splice(index, 1);
        updateSelected("exceptions", list);
    }

    function changeException(index: number, changes: Partial<OpeningException>) {
        if (!selected) return;
        const list = [...selected.exceptions];
        list[index] = {...list[index], ...changes} as OpeningException;
        updateSelected("exceptions", list);
    }

    function addExceptionPeriod(exIndex: number) {
        if (!selected) return;
        const list = [...selected.exceptions];
        const ep: OpeningExceptionPeriod = {
            startTime: "10:00",
            endTime: "14:00",
            sortOrder: list[exIndex].periods.length
        };
        list[exIndex] = {...list[exIndex], periods: [...list[exIndex].periods, ep]};
        updateSelected("exceptions", list);
    }

    function removeExceptionPeriod(exIndex: number, pIndex: number) {
        if (!selected) return;
        const list = [...selected.exceptions];
        const periods = [...list[exIndex].periods];
        periods.splice(pIndex, 1);
        list[exIndex] = {...list[exIndex], periods: periods.map((p, i) => ({...p, sortOrder: i}))};
        updateSelected("exceptions", list);
    }

    function changeExceptionPeriod(exIndex: number, pIndex: number, changes: Partial<OpeningExceptionPeriod>) {
        if (!selected) return;
        const list = [...selected.exceptions];
        const periods = [...list[exIndex].periods];
        periods[pIndex] = {...periods[pIndex], ...changes};
        list[exIndex] = {...list[exIndex], periods};
        updateSelected("exceptions", list);
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
                            {locations.map(l => (
                                <li key={String(l.id)} style={{
                                    padding: 8,
                                    borderBottom: "1px solid #eee",
                                    display: "flex",
                                    justifyContent: "space-between"
                                }}>
                                    <div style={{cursor: "pointer"}} onClick={() => loadLocation(l.id!)}>
                                        <div style={{fontWeight: 600}}>{l.name}</div>
                                        <div style={{fontSize: 12, color: "#666"}}>{l.zoneId}</div>
                                    </div>
                                    <div style={{display: "flex", alignItems: "center", gap: 6}}>
                                        <button style={btn} onClick={() => loadLocation(l.id!)}>Open</button>
                                        <button style={btn} onClick={() => deleteLocation(l.id)}>Del</button>
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

                                {/* Exceptions */}
                                <div style={{marginBottom: 12}}>
                                    <div style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center"
                                    }}>
                                        <strong>Exceptions (holidays/special days)</strong>
                                        {editing && <button style={btn} onClick={addException}>Add exception</button>}
                                    </div>

                                    {selected.exceptions.length === 0 &&
                                        <div style={{color: "#666", marginTop: 8}}>No exceptions</div>}

                                    <div style={{marginTop: 8}}>
                                        {selected.exceptions.map((ex, i) => (
                                            <div key={i}
                                                 style={{border: "1px dashed #eee", padding: 8, marginBottom: 8}}>
                                                <div style={{display: "flex", gap: 8, alignItems: "center"}}>
                                                    <input
                                                        type="date"
                                                        value={ex.date}
                                                        onChange={e => editing && changeException(i, {date: e.target.value})}
                                                        disabled={!editing}
                                                    />
                                                    <label style={{display: "flex", alignItems: "center", gap: 6}}>
                                                        <input
                                                            type="checkbox"
                                                            checked={ex.closed}
                                                            onChange={e => editing && changeException(i, {closed: e.target.checked})}
                                                            disabled={!editing}
                                                        />
                                                        Closed
                                                    </label>

                                                    <input
                                                        placeholder="note"
                                                        value={ex.note ?? ""}
                                                        onChange={e => editing && changeException(i, {note: e.target.value})}
                                                        disabled={!editing}
                                                        style={{...input, width: 220}}
                                                    />

                                                    {editing && <button style={btn}
                                                                        onClick={() => removeException(i)}>Remove</button>}
                                                </div>

                                                {/* exception periods */}
                                                {!ex.closed && (
                                                    <div style={{marginTop: 8}}>
                                                        <div style={{
                                                            display: "flex",
                                                            justifyContent: "space-between",
                                                            alignItems: "center"
                                                        }}>
                                                            <div style={{fontSize: 13, fontWeight: 600}}>Periods</div>
                                                            {editing && <button style={btn}
                                                                                onClick={() => addExceptionPeriod(i)}>Add</button>}
                                                        </div>

                                                        {ex.periods.length === 0 &&
                                                            <div style={{color: "#666", marginTop: 6}}>No periods</div>}

                                                        {ex.periods.map((p, pi) => (
                                                            <div key={pi} style={{
                                                                display: "flex",
                                                                gap: 8,
                                                                alignItems: "center",
                                                                marginTop: 6
                                                            }}>
                                                                <input
                                                                    type="time"
                                                                    value={p.startTime}
                                                                    onChange={e => editing && changeExceptionPeriod(i, pi, {startTime: e.target.value})}
                                                                    disabled={!editing}
                                                                />
                                                                <span>-</span>
                                                                <input
                                                                    type="time"
                                                                    value={p.endTime}
                                                                    onChange={e => editing && changeExceptionPeriod(i, pi, {endTime: e.target.value})}
                                                                    disabled={!editing}
                                                                />
                                                                {editing && <button style={btn}
                                                                                    onClick={() => removeExceptionPeriod(i, pi)}>Remove</button>}
                                                            </div>
                                                        ))}
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
