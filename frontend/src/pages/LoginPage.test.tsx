// src/pages/LoginPage.test.tsx
import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import LoginPage from "./LoginPage";
import {MemoryRouter} from "react-router-dom";
import {AuthContext} from "../context/AuthContext";
import {vi, type Mock} from "vitest";

// ---- helpers / mocks ----

// Use globalThis for TS compatibility
//const globalFetch = globalThis.fetch as unknown as Mock;

// mock navigate: we'll replace useNavigate in the mocked react-router-dom below
const mockNavigate = vi.fn();

// Partial-mock react-router-dom to intercept useNavigate.
// Use vi.importActual to keep other exports intact.
vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual<any>("react-router-dom");
    return {
        ...(actual as any),
        useNavigate: () => mockNavigate,
        // keep useLocation default; you can override in tests by providing state to MemoryRouter
    };
});

// Utility to render LoginPage with a custom AuthContext value
function renderWithAuthContext(value: any) {
    return render(
        <MemoryRouter>
            <AuthContext.Provider value={value}>
                <LoginPage/>
            </AuthContext.Provider>
        </MemoryRouter>
    );
}

describe("LoginPage with AuthContext", () => {
    const mockLogin = vi.fn();
    const mockLogout = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("successful login calls context.login() and navigates", async () => {
        // mock successful fetch response
        (globalThis.fetch as unknown as Mock).mockResolvedValueOnce({
            ok: true,
            json: async () => ({token: "jwt123"}),
        });

        renderWithAuthContext({
            token: null,
            isLoggedIn: false,
            login: mockLogin,
            logout: mockLogout,
            authFetch: vi.fn(),
        });

        fireEvent.change(screen.getByLabelText(/username/i), {
            target: {value: "user"},
        });
        fireEvent.change(screen.getByLabelText(/password/i), {
            target: {value: "password"},
        });

        fireEvent.click(screen.getByRole("button", {name: /login/i}));

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledWith("jwt123");
            expect(mockNavigate).toHaveBeenCalledWith("/", {replace: true});
        });
    });

    it("failed login shows error message", async () => {
        (globalThis.fetch as unknown as Mock).mockResolvedValueOnce({
            ok: false,
            text: async () => "Invalid credentials",
        });

        renderWithAuthContext({
            token: null,
            isLoggedIn: false,
            login: mockLogin,
            logout: mockLogout,
            authFetch: vi.fn(),
        });

        fireEvent.change(screen.getByLabelText(/username/i), {
            target: {value: "user"},
        });
        fireEvent.change(screen.getByLabelText(/password/i), {
            target: {value: "wrong"},
        });

        fireEvent.click(screen.getByRole("button", {name: /login/i}));

        expect(await screen.findByText(/invalid credentials/i)).toBeInTheDocument();
    });

    it("network error shows generic message", async () => {
        (globalThis.fetch as unknown as Mock).mockRejectedValueOnce(new Error("network down"));

        renderWithAuthContext({
            token: null,
            isLoggedIn: false,
            login: mockLogin,
            logout: mockLogout,
            authFetch: vi.fn(),
        });

        fireEvent.change(screen.getByLabelText(/username/i), {
            target: {value: "user"},
        });
        fireEvent.change(screen.getByLabelText(/password/i), {
            target: {value: "password"},
        });

        fireEvent.click(screen.getByRole("button", {name: /login/i}));

        expect(await screen.findByText(/network error/i)).toBeInTheDocument();
    });
});
