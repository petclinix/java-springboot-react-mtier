import { useEffect, useState } from "react";
import type { ActivityLogEntry } from "../client/dto/ActivityLogEntry.ts";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { DataTable } from "../components/ui/DataTable";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function AdminActivityLogPage() {
    const client = useApiClient();

    const [logs, setLogs] = useState<ActivityLogEntry[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchLogs();
    }, []);

    async function fetchLogs() {
        setLoading(true);
        setError(null);
        try {
            const data = await client.listActivityLogs();
            setLogs(data);
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    return (
        <PageLayout>
            <PageHeader title="Activity Log" />

            {loading && <p className="text-muted">Loading...</p>}
            {error && (
                <div className="mb-[16px]">
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}

            {!loading && !error && (
                <Card>
                    <DataTable
                        columns={[
                            { header: "Username", render: log => log.username },
                            { header: "Action", render: log => log.action },
                            { header: "Timestamp", render: log => new Date(log.timestamp).toLocaleString() },
                        ]}
                        rows={logs}
                        keyFn={log => log.id}
                        emptyMessage="No activity recorded yet."
                    />
                </Card>
            )}
        </PageLayout>
    );
}
