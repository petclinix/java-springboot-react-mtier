import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useApiClient } from "../hooks/useApiClient.ts";
import { useAuth } from "../context/AuthContext.tsx";
import type { LoginResponse } from "../client/dto/LoginResponse.tsx";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function LoginPage() {
    const client = useApiClient();
    const { signin } = useAuth();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);

    const navigate = useNavigate();
    const location = useLocation();
    const info = (location.state as any)?.info;

    async function handleLogin(e: React.FormEvent) {
        e.preventDefault();
        setError(null);

        try {
            const data: LoginResponse = await client.loginUser({ username, password });
            signin(data.token);

            // Navigate back to previous protected page or home
            const from = (location.state as any)?.from?.pathname || "/";
            navigate(from, { replace: true });
        } catch (err: any) {
            setError(err.message || "Unknown error");
        }
    }

    return (
        <PageLayout narrow>
            <PageHeader title="Login" />
            {info && <StatusMessage variant="success">{info}</StatusMessage>}
            <Card style={{ marginTop: info ? 16 : 0 }}>
                <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                        <label htmlFor="username" style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>Username</label>
                        <Input
                            id="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>
                    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                        <label htmlFor="password" style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>Password</label>
                        <Input
                            type="password"
                            id="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <Button type="submit" variant="primary">Login</Button>
                </form>
            </Card>
            {error && (
                <div style={{ marginTop: 16 }}>
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}
        </PageLayout>
    );
}
