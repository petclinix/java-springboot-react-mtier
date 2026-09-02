import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/auth.ts";
import { PageLayout } from "../components/ui/PageLayout";

export default function LogoutPage() {
    const { signout } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        signout();
        navigate("/", { replace: true });
    }, [navigate, signout]);

    return (
        <PageLayout narrow>
            <p className="text-center text-muted pt-[48px]">Signing out…</p>
        </PageLayout>
    );
}
