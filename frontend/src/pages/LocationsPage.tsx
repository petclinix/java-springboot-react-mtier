import {useEffect, useState} from "react";
import type {
    Location,
    OpeningOverride,
    OpeningPeriod
} from "../client/dto/Location.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";
import {PageHeader} from "../components/ui/PageHeader";
import {Card} from "../components/ui/Card";
import {Button} from "../components/ui/Button";
import {StatusMessage} from "../components/ui/StatusMessage";

const days = [
    {v: 1, label: "Mon"},
    {v: 2, label: "Tue"},
    {v: 3, label: "Wed"},
    {v: 4, label: "Thu"},
    {v: 5, label: "Fri"},
    {v: 6, label: "Sat"},
    {v: 7, label: "Sun"},
];

const inputClass = "w-full px-[12px] py-[8px] text-[14px] border border-strong rounded-card bg-surface text-[#1e293b] font-[inherit] box-border";
const smallInputClass = "px-[8px] py-[6px] text-[13px] w-[120px] border border-strong rounded-card font-[inherit] box-border";

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
        <div className="max-w-[1100px] mx-auto px-[20px] py-[32px]">
            <PageHeader title="Locations"/>

            {error && (
                <div className="mb-[16px]">
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}

            <div className="flex gap-[16px]">
                {/* Left panel — location list */}
                <div className="flex-[0_0_320px]">
                    <Card>
                        <div className="mb-[12px] flex justify-between items-center">
                            <strong className="text-[14px] font-semibold">All locations</strong>
                            <div className="flex gap-[6px]">
                                <Button size="sm" variant="primary" onClick={newLocation}>New</Button>
                                <Button size="sm" variant="secondary" onClick={fetchLocations}>Refresh</Button>
                            </div>
                        </div>

                        {loading && <p className="text-muted text-[14px]">Loading…</p>}
                        {!loading && locations.length === 0 && (
                            <p className="text-muted text-[14px]">No locations yet.</p>
                        )}

                        <ul className="list-none p-0 max-h-[600px] overflow-auto m-0">
                            {locations.map(location => (
                                <li
                                    key={String(location.id)}
                                    className="py-[8px] border-b border-default flex justify-between items-center"
                                >
                                    <div
                                        className="cursor-pointer text-[14px]"
                                        onClick={() => loadLocation(location.id!)}
                                    >
                                        {location.name}
                                    </div>
                                    <div className="flex gap-[4px]">
                                        <Button size="sm" variant="secondary"
                                                onClick={() => loadLocation(location.id!)}>Open</Button>
                                        <Button size="sm" variant="danger"
                                                onClick={() => deleteLocation(location.id)}>Del</Button>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </Card>
                </div>

                {/* Right panel — detail/edit */}
                <div className="flex-1">
                    <Card>
                        {!selected && (
                            <p className="text-muted text-[14px]">
                                Select a location to view/edit or click New.
                            </p>
                        )}

                        {selected && (
                            <>
                                <div className="flex justify-between items-center mb-[16px]">
                                    <h2 className="m-0 text-[18px] font-semibold">
                                        {selected.id ? `#${selected.id} ${selected.name}` : "New location"}
                                    </h2>
                                    <div className="flex gap-[6px]">
                                        {!editing && <Button size="sm" variant="secondary"
                                                             onClick={() => setEditing(true)}>Edit</Button>}
                                        {editing && (
                                            <Button size="sm" variant="secondary" onClick={() => {
                                                setSelected(null);
                                                setEditing(false);
                                            }}>Cancel</Button>
                                        )}
                                        <Button size="sm" variant="danger"
                                                onClick={() => selected && deleteLocation(selected.id)}>Delete</Button>
                                    </div>
                                </div>

                                {/* Address fields */}
                                {[
                                    {key: "name" as keyof Location, label: "Name"},
                                    {key: "zoneId" as keyof Location, label: "Zone ID"},
                                    {key: "street" as keyof Location, label: "Street"},
                                    {key: "postalCode" as keyof Location, label: "Postal Code"},
                                    {key: "city" as keyof Location, label: "City"},
                                    {key: "country" as keyof Location, label: "Country"},
                                ].map(({key, label}) => (
                                    <div key={key} className="mb-[12px]">
                                        <label className="text-[12px] font-semibold text-muted">{label}</label>
                                        <input
                                            className={inputClass}
                                            value={(selected[key] as string) ?? ""}
                                            onChange={e => editing ? updateSelected(key, e.target.value as Location[typeof key]) : undefined}
                                            disabled={!editing}
                                        />
                                    </div>
                                ))}

                                {/* Weekly Periods */}
                                <div className="mb-[12px]">
                                    <div className="flex justify-between items-center mb-[8px]">
                                        <strong className="text-[14px]">Weekly periods</strong>
                                        {editing && <Button size="sm" variant="secondary" onClick={addWeeklyPeriod}>Add
                                            period</Button>}
                                    </div>

                                    {selected.weeklyPeriods.length === 0 && (
                                        <p className="text-muted text-[13px]">No weekly periods</p>
                                    )}

                                    <div>
                                        {selected.weeklyPeriods.map((p, idx) => (
                                            <div key={idx} className="flex gap-[8px] items-center mb-[6px]">
                                                <select
                                                    value={p.dayOfWeek}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, {dayOfWeek: Number(e.target.value)})}
                                                    disabled={!editing}
                                                    className="px-[8px] py-[6px] rounded-card border border-strong font-[inherit]"
                                                >
                                                    {days.map(d => <option value={d.v} key={d.v}>{d.label}</option>)}
                                                </select>

                                                <input
                                                    type="time"
                                                    value={p.startTime}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, {startTime: e.target.value})}
                                                    className={smallInputClass}
                                                    disabled={!editing}
                                                />
                                                <span className="text-muted">-</span>
                                                <input
                                                    type="time"
                                                    value={p.endTime}
                                                    onChange={e => editing && changeWeeklyPeriod(idx, {endTime: e.target.value})}
                                                    className={smallInputClass}
                                                    disabled={!editing}
                                                />

                                                <Button size="sm" variant="danger"
                                                        onClick={() => removeWeeklyPeriod(idx)} disabled={!editing}>
                                                    Remove
                                                </Button>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Overrides */}
                                <div className="mb-[12px]">
                                    <div className="flex justify-between items-center mb-[8px]">
                                        <strong className="text-[14px]">Overrides (holidays/special days)</strong>
                                        {editing && <Button size="sm" variant="secondary" onClick={addOverride}>Add
                                            override</Button>}
                                    </div>

                                    {selected.overrides.length === 0 && (
                                        <p className="text-muted text-[13px]">No overrides</p>
                                    )}

                                    <div>
                                        {selected.overrides.map((ov, i) => (
                                            <div
                                                key={i}
                                                className="border border-dashed border-default p-[8px] mb-[8px] rounded-card"
                                            >
                                                <div className="flex gap-[8px] items-center">
                                                    <input
                                                        type="date"
                                                        value={ov.date}
                                                        onChange={e => editing && changeOverride(i, {date: e.target.value})}
                                                        disabled={!editing}
                                                        className="px-[8px] py-[6px] rounded-card border border-strong font-[inherit]"
                                                    />
                                                    <label className="flex items-center gap-[6px] text-[14px]">
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
                                                        className={`${inputClass} w-[220px]`}
                                                    />

                                                    {editing && (
                                                        <Button size="sm" variant="danger"
                                                                onClick={() => removeOverride(i)}>Remove</Button>
                                                    )}
                                                </div>

                                                {!ov.closed && (
                                                    <div className="flex gap-[8px] items-center mt-[6px]">
                                                        <input
                                                            type="time"
                                                            value={ov.openTime ?? ""}
                                                            onChange={e => editing && changeOverride(i, {openTime: e.target.value})}
                                                            disabled={!editing}
                                                            className={smallInputClass}
                                                        />
                                                        <span className="text-muted">-</span>
                                                        <input
                                                            type="time"
                                                            value={ov.closeTime ?? ""}
                                                            onChange={e => editing && changeOverride(i, {closeTime: e.target.value})}
                                                            disabled={!editing}
                                                            className={smallInputClass}
                                                        />
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Actions */}
                                <div className="flex gap-[8px]">
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

                                    <Button variant="secondary" onClick={() => {
                                        setSelected(null);
                                    }}>Close</Button>
                                </div>
                            </>
                        )}
                    </Card>
                </div>
            </div>
        </div>
    );
}
