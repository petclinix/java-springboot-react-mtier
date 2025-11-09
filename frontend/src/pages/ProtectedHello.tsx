import React, { useEffect, useState } from "react";
import { getToken, logout } from "../utils/auth";

export default function ProtectedHello() {
    const [message, setMessage] = useState("");

    useEffect(() => {
        async function fetchProtected() {
            try {
                const token = getToken();
                const res = await fetch("/api/protected/hello", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                const text = await res.text();
                setMessage(text);
            } catch {
                setMessage("Error fetching protected resource");
            }
        }

        fetchProtected();
    }, []);

    return (
        <div>
            <h2>Hello</h2>
            <p>{message}</p>
            <button onClick={() => logout()}>Logout</button>
        </div>
    );
}
