import React, {createContext, useContext, useEffect, useMemo, useState} from "react";
import {jwtDecode} from "jwt-decode";

export type Role = "ADMIN" | "VET" | "OWNER";

export class User {
    id: number;
    username: string;
    roles: Role[];

    constructor(id: number, username: string, roles: Role[]) {
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    hasRole(role: string): boolean {
        return this.roles.includes(role as Role);
    }
};

type AuthContextType = {
    user: User | null;
    token: string | null;
    signin: (jwt: string) => void;
    signout: () => void;
    hasRole: (role: Role | Role[]) => boolean;
};

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
    return ctx;
};

function decodeUser(jwt: string | null): User | null {
    if (!jwt) return null;
    try {
        const decoded: any = jwtDecode(jwt);
        return new User(decoded.sub, decoded.username ?? decoded.sub, decoded.scope || []);
    } catch {
        return null;
    }
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [token, setToken] = useState<string | null>(() => localStorage.getItem("jwt"));
    const [user, setUser] = useState<User | null>(() => decodeUser(localStorage.getItem("jwt")));

    // Keep user in sync when token changes after initial mount
    useEffect(() => {
        setUser(decodeUser(token));
    }, [token]);

    const signin = (jwt: string) => {
        localStorage.setItem("jwt", jwt);
        setToken(jwt);
    };

    const signout = () => {
        localStorage.removeItem("jwt");
        setToken(null);
        setUser(null);
    };

    const hasRole = (allowed: Role | Role[]) => {
        if (!user) return false;
        const arr = Array.isArray(allowed) ? allowed : [allowed];
        return arr.some((r) => user.roles.includes(r));
    };

    const value = useMemo(
        () => ({ user, token, signin, signout, hasRole }),
        [user, token, signin, signout, hasRole]
    );

    return (
       <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};
