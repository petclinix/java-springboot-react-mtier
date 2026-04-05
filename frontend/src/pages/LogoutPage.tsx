import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.tsx";
import { PageLayout } from "../components/ui/PageLayout";

export default function LogoutPage() {
    const { signout } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        signout();
        navigate("/", { replace: true });
    }, []);

    return (
        <PageLayout narrow>
            <p style={{ textAlign: "center", color: "var(--color-text-muted)", paddingTop: 48 }}>Signing out…</p>
        </PageLayout>
    );
}
