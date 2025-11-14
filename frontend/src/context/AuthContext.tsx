import React, { createContext, useContext, useState, useEffect } from "react";

type AuthContextType = {
    token: string | null;
    isLoggedIn: boolean;
    login: (token: string) => void;
    logout: () => void;
};

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [token, setToken] = useState<string | null>(null);

    // Load token from localStorage on startup
    useEffect(() => {
        const stored = localStorage.getItem("authToken");
        if (stored) setToken(stored);
    }, []);

    function login(newToken: string) {
        localStorage.setItem("authToken", newToken);
        setToken(newToken);
    }

    function logout() {
        localStorage.removeItem("authToken");
        setToken(null);
    }

    return (
        <AuthContext.Provider
            value={{
                token,
                isLoggedIn: !!token,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
    return ctx;
}
