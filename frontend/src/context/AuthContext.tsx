import React, {useCallback, useEffect, useMemo, useState} from "react";
import {jwtDecode} from "jwt-decode";
import {AuthContext, User, type Role} from "./auth";

interface DecodedJwt {
    sub: number;
    username?: string;
    scope?: Role | Role[];
}

function decodeUser(jwt: string | null): User | null {
    if (!jwt) return null;
    try {
        const decoded = jwtDecode<DecodedJwt>(jwt);
        const scope = decoded.scope;
        const roles: Role[] = Array.isArray(scope) ? scope : (scope ? [scope] : []);
        return new User(decoded.sub, decoded.username ?? String(decoded.sub), roles);
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

    const signin = useCallback((jwt: string) => {
        localStorage.setItem("jwt", jwt);
        setToken(jwt);
    }, []);

    const signout = useCallback(() => {
        localStorage.removeItem("jwt");
        setToken(null);
        setUser(null);
    }, []);

    const hasRole = useCallback((allowed: Role | Role[]) => {
        if (!user) return false;
        const arr = Array.isArray(allowed) ? allowed : [allowed];
        return arr.some((r) => user.roles.includes(r));
    }, [user]);

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
