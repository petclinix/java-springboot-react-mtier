import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginPage from "./LoginPage";
import { MemoryRouter } from "react-router-dom";
import * as auth from "../utils/auth";
import { vi, type Mock } from "vitest";

// Mock navigate and location via react-router-dom partial mock
const mockedNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual<any>("react-router-dom");
    return {
        ...actual,
        useNavigate: () => mockedNavigate,
        useLocation: () => ({ state: { from: { pathname: "/dashboard" } } }),
    };
});

// Spy setToken so we can assert it got called
vi.spyOn(auth, "setToken").mockImplementation(vi.fn());

// Mock fetch globally
(globalThis as any).fetch = vi.fn();

describe("LoginPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    test("successful login stores token and navigates back", async () => {
        (globalThis.fetch as unknown as Mock).mockResolvedValueOnce({
            ok: true,
            json: async () => ({ token: "jwt123", type: "Bearer" }),
        });

        render(
            <MemoryRouter>
                <LoginPage />
            </MemoryRouter>
        );

        const user = userEvent.setup();
        await user.type(screen.getByLabelText(/username/i), "user");
        await user.type(screen.getByLabelText(/password/i), "password");
        await user.click(screen.getByRole("button", { name: /login/i }));

        await waitFor(() => {
            expect(auth.setToken).toHaveBeenCalledWith("jwt123");
            expect(mockedNavigate).toHaveBeenCalledWith("/dashboard", { replace: true });
        });
    });

    test("failed login shows error message", async () => {
        (globalThis.fetch as unknown as Mock).mockResolvedValueOnce({
            ok: false,
            text: async () => "Invalid credentials",
        });

        render(
            <MemoryRouter>
                <LoginPage />
            </MemoryRouter>
        );

        const user = userEvent.setup();
        await user.type(screen.getByLabelText(/username/i), "user");
        await user.type(screen.getByLabelText(/password/i), "wrong");
        await user.click(screen.getByRole("button", { name: /login/i }));

        const errorMessage = await screen.findByText(/invalid credentials/i);
        expect(errorMessage).toBeInTheDocument();
    });

    test("network error displays generic error", async () => {
        (globalThis.fetch as unknown as Mock).mockRejectedValueOnce(new Error("Network broke"));

        render(
            <MemoryRouter>
                <LoginPage />
            </MemoryRouter>
        );

        const user = userEvent.setup();
        await user.type(screen.getByLabelText(/username/i), "user");
        await user.type(screen.getByLabelText(/password/i), "password");
        await user.click(screen.getByRole("button", { name: /login/i }));

        const error = await screen.findByText(/network error/i);
        expect(error).toBeInTheDocument();
    });
});
