import {createContext, useContext} from "react";

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
}

export type AuthContextType = {
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
