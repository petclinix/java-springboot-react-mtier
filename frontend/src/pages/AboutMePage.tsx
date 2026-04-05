import {useEffect, useState} from "react";
import type {UserResponse} from "../client/dto/UserResponse.tsx";
import {useApiClient} from "../hooks/useApiClient.ts";


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
        <div>
            <p>{message}</p>
            <h2>Hello</h2>
            <p>{userResponse?.id}</p>
            <p>{userResponse?.username}</p>
            <p>Role: <span>{userResponse?.role}</span></p>
        </div>
    );
}
