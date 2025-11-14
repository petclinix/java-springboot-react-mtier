import {useEffect} from "react";
import {logout} from "../utils/auth";
import {useNavigate} from "react-router-dom";

export default function LogoutPage() {
    const navigate = useNavigate();

    useEffect(() => {
        logout();
        navigate("/", {replace: true});
    }, []);

    return <p>Logging out...</p>;
}
