import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import RegisterPage from "./RegisterPage";
import {apiClient} from "../client/ApiClient";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual<any>("react-router-dom");
    return {
        ...(actual as any),
        useNavigate: () => mockNavigate,
    };
});

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        registerUser: vi.fn(),
    },
}));

function renderPage() {
    return render(
        <MemoryRouter>
            <RegisterPage />
        </MemoryRouter>
    );
}

describe("RegisterPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders the registration form", () => {
        // arrange
        // act
        renderPage();

        // assert
        expect(screen.getByRole("heading", {name: "Register"})).toBeInTheDocument();
        expect(screen.getByLabelText("username")).toBeInTheDocument();
        expect(screen.getByLabelText("password")).toBeInTheDocument();
        expect(screen.getByLabelText("type")).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /register/i})).toBeInTheDocument();
    });

    it("shows error when username is too short", async () => {
        // arrange
        renderPage();

        // act
        fireEvent.change(screen.getByLabelText("username"), {target: {value: "ab"}});
        fireEvent.change(screen.getByLabelText("password"), {target: {value: "abc"}});
        fireEvent.click(screen.getByRole("button", {name: /^register$/i}));

        // assert
        expect(await screen.findByText(/username must be at least 3 characters/i)).toBeInTheDocument();
        expect(apiClient.registerUser).not.toHaveBeenCalled();
    });

    it("shows error when password is too short", async () => {
        // arrange
        renderPage();

        // act
        fireEvent.change(screen.getByLabelText("username"), {target: {value: "alice"}});
        fireEvent.change(screen.getByLabelText("password"), {target: {value: "ab"}});
        fireEvent.click(screen.getByRole("button", {name: /^register$/i}));

        // assert
        expect(await screen.findByText(/password must be at least 3 characters/i)).toBeInTheDocument();
        expect(apiClient.registerUser).not.toHaveBeenCalled();
    });

    it("successful registration navigates to /login", async () => {
        // arrange
        (apiClient.registerUser as ReturnType<typeof vi.fn>).mockResolvedValue({
            ok: true,
            text: async () => "",
        });
        renderPage();

        // act
        fireEvent.change(screen.getByLabelText("username"), {target: {value: "alice"}});
        fireEvent.change(screen.getByLabelText("password"), {target: {value: "secret"}});
        fireEvent.click(screen.getByRole("button", {name: /^register$/i}));

        // assert
        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith(
                "/login",
                expect.objectContaining({replace: true})
            );
        });
        expect(apiClient.registerUser).toHaveBeenCalledWith({
            username: "alice",
            password: "secret",
            type: "owner",
        });
    });

    it("server error message is displayed", async () => {
        // arrange
        (apiClient.registerUser as ReturnType<typeof vi.fn>).mockResolvedValue({
            ok: false,
            text: async () => "Username already taken",
        });
        renderPage();

        // act
        fireEvent.change(screen.getByLabelText("username"), {target: {value: "alice"}});
        fireEvent.change(screen.getByLabelText("password"), {target: {value: "secret"}});
        fireEvent.click(screen.getByRole("button", {name: /^register$/i}));

        // assert
        expect(await screen.findByText("Username already taken")).toBeInTheDocument();
        expect(mockNavigate).not.toHaveBeenCalled();
    });
});
