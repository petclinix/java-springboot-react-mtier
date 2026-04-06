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
            <Card className={info ? "mt-[16px]" : ""}>
                <form onSubmit={handleLogin} className="flex flex-col gap-[16px]">
                    <div className="flex flex-col gap-[4px]">
                        <label htmlFor="username" className="text-[13px] font-semibold text-[#1e293b]">Username</label>
                        <Input
                            id="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>
                    <div className="flex flex-col gap-[4px]">
                        <label htmlFor="password" className="text-[13px] font-semibold text-[#1e293b]">Password</label>
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
                <div className="mt-[16px]">
                    <StatusMessage variant="error">{error}</StatusMessage>
                </div>
            )}
        </PageLayout>
    );
}
