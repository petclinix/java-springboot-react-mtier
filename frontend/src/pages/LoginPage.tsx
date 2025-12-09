import React, { useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import {useApiClient} from "../hooks/useApiClient.ts";
import {useAuth} from "../context/AuthContext.tsx";
import type {LoginResponse} from "../client/dto/LoginResponse.tsx";

export default function LoginPage() {
    const client = useApiClient();
    const {signin} = useAuth();
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
            const data: LoginResponse = await client.loginUser({username, password});
            signin(data.token);

            // Navigate back to previous protected page or home
            const from = (location.state as any)?.from?.pathname || "/";
            navigate(from, {replace: true});
        } catch (err) {
            setError("Network error");
        }
    }

    return (
        <div style={{maxWidth: 400, margin: "3rem auto"}}>
            <h2>Login</h2>
            {info && <p style={{color: "green"}}>{info}</p>}
            <form onSubmit={handleLogin}>
                <div>
                    <label htmlFor="username">Username</label>
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
            {error && <p style={{color: "red"}}>{error}</p>}
        </div>
    );
}
