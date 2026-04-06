import { useEffect, useState } from "react";
import type { AdminUser } from "../client/dto/AdminUser.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { useAuth } from "../context/AuthContext.tsx";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { DataTable } from "../components/ui/DataTable";
import { StatusMessage } from "../components/ui/StatusMessage";

function roleBadgeVariant(role: string): "owner" | "vet" | "admin" | "neutral" {
    const r = role?.toLowerCase();
    if (r === "owner") return "owner";
    if (r === "vet") return "vet";
    if (r === "admin") return "admin";
    return "neutral";
}

export default function AdminUsersPage() {
    const client = useApiClient();
    const { user: currentUser } = useAuth();

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

    return (
        <PageLayout>
            <PageHeader title="All Users" />

            {loading && <p className="text-muted">Loading...</p>}
            {error && (
                <div className="mb-[16px]">
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}

            {!loading && (
                <Card>
                    <DataTable
                        columns={[
                            {
                                header: "Username",
                                render: u => u.username,
                            },
                            {
                                header: "Role",
                                render: u => (
                                    <Badge variant={roleBadgeVariant(u.role)}>{u.role}</Badge>
                                ),
                            },
                            {
                                header: "Status",
                                render: u => (
                                    <Badge variant={u.active ? "active" : "inactive"}>
                                        {u.active ? "Active" : "Deactivated"}
                                    </Badge>
                                ),
                            },
                            {
                                header: "Actions",
                                render: u => (
                                    <>
                                        {u.username !== currentUser?.username && u.active && (
                                            <Button
                                                variant="danger"
                                                size="sm"
                                                disabled={deactivating === u.id}
                                                onClick={() => handleDeactivate(u.id)}
                                            >
                                                {deactivating === u.id ? "Deactivating..." : "Deactivate"}
                                            </Button>
                                        )}
                                        {u.username !== currentUser?.username && !u.active && (
                                            <Button
                                                variant="secondary"
                                                size="sm"
                                                disabled={activating === u.id}
                                                onClick={() => handleActivate(u.id)}
                                            >
                                                {activating === u.id ? "Activating..." : "Activate"}
                                            </Button>
                                        )}
                                    </>
                                ),
                            },
                        ]}
                        rows={users}
                        keyFn={u => u.id}
                    />
                </Card>
            )}
        </PageLayout>
    );
}
