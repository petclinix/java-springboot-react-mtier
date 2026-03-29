import React, {useEffect, useState} from "react";
import type {AdminUser} from "../client/dto/AdminUser.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";
import {useAuth} from "../context/AuthContext.tsx";

export default function AdminUsersPage() {
    const client = useApiClient();
    const {user: currentUser} = useAuth();

    const [users, setUsers] = useState<AdminUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [deactivating, setDeactivating] = useState<number | null>(null);
    const [activating, setActivating] = useState<number | null>(null);

    useEffect(() => {
        fetchUsers();
    }, []);

    async function fetchUsers() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.listAllUsers();
            setUsers(data);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    async function handleDeactivate(id: number) {
        setDeactivating(id);
        setError(null);
        try {
            const updated = await client.deactivateUser(id);
            setUsers(prev => prev.map(u => u.id === id ? updated : u));
        } catch (err: any) {
            setError(err.message || "Deactivate failed");
        } finally {
            setDeactivating(null);
        }
    }

    async function handleActivate(id: number) {
        setActivating(id);
        setError(null);
        try {
            const updated = await client.activateUser(id);
            setUsers(prev => prev.map(u => u.id === id ? updated : u));
        } catch (err: any) {
            setError(err.message || "Activate failed");
        } finally {
            setActivating(null);
        }
    }

    const button: React.CSSProperties = {
        padding: "4px 10px",
        cursor: "pointer",
    };

    return (
        <div style={{maxWidth: 800, margin: "0 auto", padding: 20}}>
            <h1>All Users</h1>

            {loading && <div>Loading...</div>}
            {error && <p style={{color: "red"}}>{error}</p>}

            {!loading && (
                <table style={{width: "100%", borderCollapse: "collapse"}}>
                    <thead>
                        <tr>
                            <th style={{textAlign: "left", padding: 8, borderBottom: "1px solid #ccc"}}>Username</th>
                            <th style={{textAlign: "left", padding: 8, borderBottom: "1px solid #ccc"}}>Role</th>
                            <th style={{textAlign: "left", padding: 8, borderBottom: "1px solid #ccc"}}>Status</th>
                            <th style={{textAlign: "left", padding: 8, borderBottom: "1px solid #ccc"}}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map(u => (
                            <tr key={u.id} style={{borderBottom: "1px solid #eee"}}>
                                <td style={{padding: 8}}>{u.username}</td>
                                <td style={{padding: 8}}>
                                    <span style={{
                                        background: "#e0e0e0",
                                        borderRadius: 4,
                                        padding: "2px 6px",
                                        fontSize: "0.85em",
                                    }}>{u.role}</span>
                                </td>
                                <td style={{padding: 8}}>
                                    {u.active ? "Active" : "Deactivated"}
                                </td>
                                <td style={{padding: 8}}>
                                    {u.username !== currentUser?.username && u.active && (
                                        <button
                                            style={button}
                                            disabled={deactivating === u.id}
                                            onClick={() => handleDeactivate(u.id)}
                                        >
                                            {deactivating === u.id ? "Deactivating..." : "Deactivate"}
                                        </button>
                                    )}
                                    {u.username !== currentUser?.username && !u.active && (
                                        <button
                                            style={button}
                                            disabled={activating === u.id}
                                            onClick={() => handleActivate(u.id)}
                                        >
                                            {activating === u.id ? "Activating..." : "Activate"}
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
}
