import {jwtDecode} from "jwt-decode";

const TOKEN_KEY = "token";

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
}

export function logout(): void {
    localStorage.removeItem(TOKEN_KEY);
}

// Decode JWT and check if expired
export function isLoggedIn(): boolean {
    const token = getToken();
    if (!token) return false;

    try {
        const decoded: { exp: number } = jwtDecode(token);
        return decoded.exp * 1000 > Date.now();
    } catch {
        return false;
    }
}
