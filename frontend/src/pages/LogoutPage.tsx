import {useEffect} from "react";
import {useNavigate} from "react-router-dom";
import {useAuth} from "../context/AuthContext.tsx";

export default function LogoutPage() {
    const { signout } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        signout();
        navigate("/", {replace: true});
    }, []);

    return <p>Logging out...</p>;
}
