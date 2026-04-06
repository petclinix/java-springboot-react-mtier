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
                    <div className="flex flex-col gap-[12px]">
                        <div>
                            <span className="text-[12px] font-semibold text-muted uppercase tracking-[0.05em]">ID</span>
                            <p className="mt-[4px] mb-0 text-[15px] text-[#1e293b]">{userResponse.id}</p>
                        </div>
                        <div>
                            <span className="text-[12px] font-semibold text-muted uppercase tracking-[0.05em]">Username</span>
                            <p className="mt-[4px] mb-0 text-[15px] font-semibold text-[#1e293b]">{userResponse.username}</p>
                        </div>
                        <div>
                            <span className="text-[12px] font-semibold text-muted uppercase tracking-[0.05em]">Role</span>
                            <p className="mt-[4px] mb-0">
                                <Badge variant={roleBadgeVariant(userResponse.role)}><span>{userResponse.role}</span></Badge>
                            </p>
                        </div>
                    </div>
                </Card>
            )}
        </PageLayout>
    );
}
