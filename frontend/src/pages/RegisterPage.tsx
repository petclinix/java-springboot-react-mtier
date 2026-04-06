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
                <form onSubmit={handleSubmit} aria-label="registration-form" className="flex flex-col gap-[16px]">
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
                    <div className="mt-[16px]">
                        <StatusMessage variant="error">{error}</StatusMessage>
                    </div>
                )}

                <p className="mt-[16px] text-[14px] text-muted">
                    Already have an account?{" "}
                    <Button
                        variant="ghost"
                        onClick={() => navigate("/login")}
                        aria-label="go-to-login"
                        className="text-[14px] p-0"
                    >
                        Log in
                    </Button>
                </p>
            </Card>
        </PageLayout>
    );
}
