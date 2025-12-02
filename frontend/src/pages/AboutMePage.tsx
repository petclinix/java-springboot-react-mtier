import { useEffect, useState } from "react";
import {useAuth} from "../context/AuthContext.tsx";

type UserResponse = {
    id: number,
    username: string,
    owner: boolean
};


export default function AboutMePage() {
    const [userResponse, setUserResponse] = useState<UserResponse | null>(null);
    const [message, setMessage] = useState("");
    const { token } = useAuth();

    useEffect(() => {
        async function fetchAboutMe() {
            try {
                fetch("/api/users/aboutme", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                })
                    .then(res => res.json())
                    .then((data: UserResponse) => {
                        setUserResponse(data);
                    });
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
            <p>Is owner? {userResponse?.owner? "Owner": "Vet"}</p>
        </div>
    );
}
