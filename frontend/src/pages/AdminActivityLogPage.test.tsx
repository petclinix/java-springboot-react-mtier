import {render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AdminActivityLogPage from "./AdminActivityLogPage";
import {apiClient} from "../client/ApiClient";
import {AuthContext, User} from "../context/AuthContext";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listActivityLogs: vi.fn(),
    }
}));

const ADMIN_USER = new User(1, "admin", ["ADMIN"]);

const LOGS = [
    {id: 1, username: "grace", action: "USER_LOGIN", timestamp: "2026-08-02T10:15:30"},
    {id: 2, username: "bob", action: "USER_LOGOUT", timestamp: "2026-08-01T09:00:00"},
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
                <AdminActivityLogPage/>
            </AuthContext.Provider>
        </MemoryRouter>
    );
}

describe("AdminActivityLogPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders page heading", async () => {
        // arrange
        (apiClient.listActivityLogs as any).mockResolvedValue(LOGS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Activity Log")).toBeInTheDocument();
    });

    it("shows loading state", async () => {
        // arrange
        let resolve!: (v: any) => void;
        (apiClient.listActivityLogs as any).mockReturnValue(new Promise(r => { resolve = r; }));

        // act
        renderPage();

        // assert
        expect(screen.getByText("Loading...")).toBeInTheDocument();

        resolve(LOGS);
        await waitFor(() => {
            expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
        });
    });

    it("renders activity log entries", async () => {
        // arrange
        (apiClient.listActivityLogs as any).mockResolvedValue(LOGS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("grace")).toBeInTheDocument();
        expect(screen.getByText("USER_LOGIN")).toBeInTheDocument();
        expect(screen.getByText(new Date("2026-08-02T10:15:30").toLocaleString())).toBeInTheDocument();
        expect(screen.getByText("bob")).toBeInTheDocument();
        expect(screen.getByText("USER_LOGOUT")).toBeInTheDocument();
        expect(screen.getByText(new Date("2026-08-01T09:00:00").toLocaleString())).toBeInTheDocument();
    });

    it("shows empty state when there are no entries", async () => {
        // arrange
        (apiClient.listActivityLogs as any).mockResolvedValue([]);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("No activity recorded yet.")).toBeInTheDocument();
    });

    it("shows error on fetch failure", async () => {
        // arrange
        (apiClient.listActivityLogs as any).mockRejectedValue(new Error("Network error"));

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });
});
