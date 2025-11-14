import { useEffect, useState } from "react";
import {useAuth} from "../context/AuthContext.tsx";

export default function ProtectedHello() {
    const [message, setMessage] = useState("");
    const { token } = useAuth();

    useEffect(() => {
        async function fetchProtected() {
            try {
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
        </div>
    );
}
