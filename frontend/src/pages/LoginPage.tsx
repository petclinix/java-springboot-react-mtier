import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {useAuth} from "../context/AuthContext.tsx";

interface LoginResponse {
    token: string;
    type: string;
}

export default function LoginPage() {
    const { login } = useAuth();
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
            const res = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            if (!res.ok) {
                const text = await res.text();
                setError(text || "Login failed");
                return;
            }

            const data: LoginResponse = await res.json();
            login(data.token);

            // Navigate back to previous protected page or home
            const from = (location.state as any)?.from?.pathname || "/";
            navigate(from, { replace: true });
        } catch (err) {
            setError("Network error");
        }
    }

    return (
        <div style={{ maxWidth: 400, margin: "3rem auto" }}>
            <h2>Login</h2>
            {info && <p style={{ color: "green" }}>{info}</p>}
            <form onSubmit={handleLogin}>
                <div>
                    <label htmlFor="username" >Username</label>
                    <input
                        id="username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="password">Password</label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                <button type="submit">Login</button>
            </form>
            {error && <p style={{ color: "red" }}>{error}</p>}
        </div>
    );
}
