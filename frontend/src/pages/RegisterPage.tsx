import React, { type JSX, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApiClient } from "../hooks/useApiClient.ts";
import { PageLayout } from "../components/ui/PageLayout";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { FormField } from "../components/ui/FormField";
import { Input } from "../components/ui/Input";
import { Select } from "../components/ui/Select";
import { Button } from "../components/ui/Button";
import { StatusMessage } from "../components/ui/StatusMessage";

export default function RegisterPage(): JSX.Element {
    const client = useApiClient();

    const [username, setUsername] = useState<string>("");
    const [password, setPassword] = useState<string>("");
    const [userType, setUserType] = useState<string>("owner");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const navigate = useNavigate();

    // basic client-side validation rules
    const usernameValid = username.trim().length >= 3;
    const passwordValid = password.length >= 3;

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setError(null);

        if (!usernameValid) {
            setError("Username must be at least 3 characters.");
            return;
        }
        if (!passwordValid) {
            setError("Password must be at least 3 characters.");
            return;
        }

        setLoading(true);
        try {
            const res = await client.registerUser({
                username: username.trim(),
                password,
                type: userType
            });

            if (res.ok) {
                navigate("/login", {
                    state: { info: "Registration successful — please log in." },
                    replace: true,
                });
            } else {
                // try to get server-provided error message
                const text = await res.text();
                setError(text || "Registration failed");
            }
        } catch (err: any) {
            setError(err.message || "Unknown error");
        } finally {
            setLoading(false);
        }
    }

    return (
        <PageLayout narrow>
            <PageHeader title="Register" />
            <Card>
                <form onSubmit={handleSubmit} aria-label="registration-form" style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                    <FormField label="Username">
                        <Input
                            aria-label="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="min 3 characters"
                            required
                        />
                    </FormField>

                    <FormField label="Password">
                        <Input
                            aria-label="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="min 3 characters"
                            required
                        />
                    </FormField>

                    <FormField label="User Type">
                        <Select
                            aria-label="type"
                            value={userType}
                            onChange={(e) => setUserType(e.target.value)}
                        >
                            <option value="owner">Pet Owner</option>
                            <option value="vet">Veterinarian</option>
                        </Select>
                    </FormField>

                    <Button
                        type="submit"
                        variant="primary"
                        loading={loading}
                        aria-busy={loading}
                    >
                        {loading ? "Registering..." : "Register"}
                    </Button>
                </form>

                {error && (
                    <div style={{ marginTop: 16 }}>
                        <StatusMessage variant="error">{error}</StatusMessage>
                    </div>
                )}

                <p style={{ marginTop: 16, fontSize: 14, color: "var(--color-text-muted)" }}>
                    Already have an account?{" "}
                    <Button
                        variant="ghost"
                        onClick={() => navigate("/login")}
                        aria-label="go-to-login"
                        style={{ fontSize: 14, padding: 0 }}
                    >
                        Log in
                    </Button>
                </p>
            </Card>
        </PageLayout>
    );
}
