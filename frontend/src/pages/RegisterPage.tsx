import React, {type JSX, useState} from "react";
import { useNavigate } from "react-router-dom";

type RegisterRequest = {
    username: string;
    password: string;
};

export default function RegisterPage(): JSX.Element {
    const [username, setUsername] = useState<string>("");
    const [password, setPassword] = useState<string>("");
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
            const payload: RegisterRequest = { username: username.trim(), password };
            const res = await fetch("/api/users/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
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
        } catch (err) {
            setError("Network error, please try again.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h2 style={styles.h}>Register</h2>

                <form onSubmit={handleSubmit} aria-label="registration-form" style={styles.form}>
                    <label style={styles.label}>
                        Username
                        <input
                            aria-label="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            style={styles.input}
                            placeholder="min 3 characters"
                            required
                        />
                    </label>

                    <label style={styles.label}>
                        Password
                        <input
                            aria-label="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            style={styles.input}
                            placeholder="min 8 characters"
                            required
                        />
                    </label>

                    <button
                        type="submit"
                        disabled={loading}
                        style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
                        aria-busy={loading}
                    >
                        {loading ? "Registering..." : "Register"}
                    </button>
                </form>

                {error && <p role="alert" style={styles.error}>{error}</p>}

                <p style={styles.small}>
                    Already have an account?{" "}
                    <button
                        onClick={() => navigate("/login")}
                        style={styles.linkButton}
                        aria-label="go-to-login"
                    >
                        Log in
                    </button>
                </p>
            </div>
        </div>
    );
}

/* minimal inline styles */
const styles: Record<string, React.CSSProperties> = {
    container: { display: "flex", justifyContent: "center", padding: "2rem" },
    card: {
        width: "100%",
        maxWidth: 420,
        padding: "1.5rem",
        border: "1px solid #e6e6e6",
        borderRadius: 8,
        boxShadow: "0 4px 12px rgba(0,0,0,0.05)",
    },
    h: { marginTop: 0, marginBottom: "1rem" },
    form: { display: "grid", gap: "0.75rem" },
    label: { display: "flex", flexDirection: "column", fontSize: 14 },
    input: {
        padding: "0.6rem",
        fontSize: 14,
        borderRadius: 4,
        border: "1px solid #ccc",
        marginTop: "0.25rem",
    },
    button: {
        padding: "0.65rem",
        borderRadius: 6,
        border: "none",
        background: "#2563eb",
        color: "white",
        fontSize: 15,
        cursor: "pointer",
        marginTop: "0.5rem",
    },
    error: { color: "crimson", marginTop: "0.75rem" },
    small: { marginTop: "1rem", fontSize: 14 },
    linkButton: {
        background: "none",
        border: "none",
        color: "#2563eb",
        textDecoration: "underline",
        cursor: "pointer",
        padding: 0,
        fontSize: 14,
    },
};
