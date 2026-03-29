import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AdminUsersPage from "./AdminUsersPage";
import {apiClient} from "../client/ApiClient";
import {AuthContext, User} from "../context/AuthContext";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listAllUsers: vi.fn(),
        deactivateUser: vi.fn(),
        activateUser: vi.fn(),
    }
}));

const ADMIN_USER = new User(1, "admin", ["ADMIN"]);

const USERS = [
    {id: 1, username: "admin", role: "ADMIN", active: true},
    {id: 2, username: "alice", role: "OWNER", active: true},
    {id: 3, username: "bob", role: "VET", active: false},
];

function renderPage(currentUser: User | null = ADMIN_USER) {
    return render(
        <MemoryRouter>
            <AuthContext.Provider value={{
                user: currentUser,
                token: "fake-token",
                signin: vi.fn(),
                signout: vi.fn(),
                hasRole: (role) => currentUser?.hasRole(Array.isArray(role) ? role[0] : role) ?? false,
            }}>
                <AdminUsersPage/>
            </AuthContext.Provider>
        </MemoryRouter>
    );
}

describe("AdminUsersPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders user list", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockResolvedValue(USERS);

        renderPage();

        // act + assert
        expect(await screen.findByText("alice")).toBeInTheDocument();
        expect(screen.getByText("bob")).toBeInTheDocument();
        expect(screen.getByText("admin")).toBeInTheDocument();
        expect(screen.getByText("OWNER")).toBeInTheDocument();
        expect(screen.getByText("VET")).toBeInTheDocument();
        expect(screen.getAllByText("Active").length).toBeGreaterThan(0);
        expect(screen.getByText("Deactivated")).toBeInTheDocument();
    });

    it("shows loading state", async () => {
        // arrange
        let resolve!: (v: any) => void;
        (apiClient.listAllUsers as any).mockReturnValue(new Promise(r => { resolve = r; }));

        renderPage();

        // act + assert
        expect(screen.getByText("Loading...")).toBeInTheDocument();

        resolve([]);
        await waitFor(() => {
            expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
        });
    });

    it("shows error on fetch failure", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockRejectedValue(new Error("Network error"));

        renderPage();

        // act + assert
        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });

    it("activate button shown for inactive users", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockResolvedValue(USERS);

        renderPage();

        await screen.findByText("bob");

        // act + assert — alice (active) gets Deactivate, bob (inactive) gets Activate, admin (current user) gets nothing
        expect(screen.getByRole("button", {name: /^deactivate$/i})).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /^activate$/i})).toBeInTheDocument();
        expect(screen.getByText("Deactivated")).toBeInTheDocument();
    });

    it("deactivate button hidden for current user", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockResolvedValue(USERS);

        renderPage(ADMIN_USER);

        await screen.findByText("admin");

        // act + assert — admin row should not have a Deactivate button
        const deactivateButtons = screen.getAllByRole("button", {name: /deactivate/i});
        // only alice gets a button; admin (current user) does not
        deactivateButtons.forEach(btn => {
            expect(btn).not.toHaveAttribute("data-username", "admin");
        });
        expect(deactivateButtons).toHaveLength(1);
    });

    it("deactivate updates user status", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockResolvedValue(USERS);
        (apiClient.deactivateUser as any).mockResolvedValue({id: 2, username: "alice", role: "OWNER", active: false});

        renderPage();

        await screen.findByText("alice");

        // act
        const deactivateBtn = screen.getByRole("button", {name: /^deactivate$/i});
        fireEvent.click(deactivateBtn);

        // assert
        await waitFor(() => {
            expect(apiClient.deactivateUser).toHaveBeenCalledWith(2);
        });
        expect(await screen.findAllByText("Deactivated")).toHaveLength(2);
    });

    it("activate updates user status", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockResolvedValue(USERS);
        (apiClient.activateUser as any).mockResolvedValue({id: 3, username: "bob", role: "VET", active: true});

        renderPage();

        await screen.findByText("bob");

        // act
        const activateBtn = screen.getByRole("button", {name: /^activate$/i});
        fireEvent.click(activateBtn);

        // assert
        await waitFor(() => {
            expect(apiClient.activateUser).toHaveBeenCalledWith(3);
        });
        expect(await screen.findAllByText("Active")).toHaveLength(3);
    });

    it("shows error on deactivate failure", async () => {
        // arrange
        (apiClient.listAllUsers as any).mockResolvedValue(USERS);
        (apiClient.deactivateUser as any).mockRejectedValue(new Error("Deactivate failed"));

        renderPage();

        await screen.findByText("alice");

        // act
        const deactivateBtn = screen.getByRole("button", {name: /^deactivate$/i});
        fireEvent.click(deactivateBtn);

        // assert
        expect(await screen.findByText("Deactivate failed")).toBeInTheDocument();
        // alice should still be shown as Active
        expect(screen.getByText("alice")).toBeInTheDocument();
    });
});
