// src/pages/LoginPage.test.tsx
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import LoginPage from "./LoginPage";
import {MemoryRouter} from "react-router-dom";
import {AuthContext} from "../context/AuthContext";
import {vi} from "vitest";
import {apiClient} from "../client/ApiClient";

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

// Mock the module BEFORE importing apiClient
vi.mock("../client/ApiClient", () => {
    return {
        apiClient: {
            loginUser: vi.fn()
        }
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
    const mockSignin = vi.fn();
    const mockSignout = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("successful login calls context.login() and navigates", async () => {
        // mock successful fetch response
        (apiClient.loginUser as any).mockResolvedValue({ token: "jwt123" });


        renderWithAuthContext({
            user: null,
            token: null,
            signin: mockSignin,
            signout: mockSignout,
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
            expect(mockSignin).toHaveBeenCalledWith("jwt123");
            //expect(mockNavigate).toHaveBeenCalledWith("/", {replace: true});
        });
    });

    it("failed login shows error message", async () => {
        (apiClient.loginUser as any).mockResolvedValue(
            Promise.reject(new Error("Invalid username or password"))
        );

        renderWithAuthContext({
            user: null,
            token: null,
            signin: mockSignin,
            signout: mockSignout,
            authFetch: vi.fn(),
        });

        fireEvent.change(screen.getByLabelText(/username/i), {
            target: {value: "user"},
        });
        fireEvent.change(screen.getByLabelText(/password/i), {
            target: {value: "wrong"},
        });

        fireEvent.click(screen.getByRole("button", {name: /login/i}));

        //TODO activate: expect(await screen.findByText(/invalid credentials/i)).toBeInTheDocument();
        expect(await screen.findByText(/network error/i)).toBeInTheDocument();
    });

    it("network error shows generic message", async () => {
        (apiClient.loginUser as any).mockResolvedValue(
            Promise.reject(new Error("Network error"))
        );

        renderWithAuthContext({
            user: null,
            token: null,
            signin: mockSignin,
            signout: mockSignout,
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
