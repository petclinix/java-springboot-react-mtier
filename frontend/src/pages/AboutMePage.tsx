import { useEffect, useState } from "react";
import type { UserResponse } from "../client/dto/UserResponse.tsx";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { StatusMessage } from "../components/ui/StatusMessage";

function roleBadgeVariant(role: string): "owner" | "vet" | "admin" | "neutral" {
    const r = role?.toLowerCase();
    if (r === "owner") return "owner";
    if (r === "vet") return "vet";
    if (r === "admin") return "admin";
    return "neutral";
}

export default function AboutMePage() {
    const client = useApiClient();

    const [userResponse, setUserResponse] = useState<UserResponse | null>(null);
    const [message, setMessage] = useState("");

    useEffect(() => {
        async function fetchAboutMe() {
            try {
                const data = await client.fetchAboutMe();
                setUserResponse(data);
            } catch {
                setMessage("Error fetching protected resource");
            }
        }

        fetchAboutMe();
    }, []);

    return (
        <PageLayout narrow>
            <PageHeader title="About Me" />
            {message && <StatusMessage variant="error">{message}</StatusMessage>}
            {userResponse && (
                <Card>
                    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                        <div>
                            <span style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-muted)", textTransform: "uppercase", letterSpacing: "0.05em" }}>ID</span>
                            <p style={{ margin: "4px 0 0", fontSize: 15, color: "var(--color-text)" }}>{userResponse.id}</p>
                        </div>
                        <div>
                            <span style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-muted)", textTransform: "uppercase", letterSpacing: "0.05em" }}>Username</span>
                            <p style={{ margin: "4px 0 0", fontSize: 15, fontWeight: 600, color: "var(--color-text)" }}>{userResponse.username}</p>
                        </div>
                        <div>
                            <span style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-muted)", textTransform: "uppercase", letterSpacing: "0.05em" }}>Role</span>
                            <p style={{ margin: "4px 0 0" }}>
                                <Badge variant={roleBadgeVariant(userResponse.role)}><span>{userResponse.role}</span></Badge>
                            </p>
                        </div>
                    </div>
                </Card>
            )}
        </PageLayout>
    );
}
