import {render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AdminDashboardPage from "./AdminDashboardPage";
import {apiClient} from "../client/ApiClient";
import {AuthContext, User} from "../context/AuthContext";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        getStats: vi.fn(),
    }
}));

const ADMIN_USER = new User(1, "admin", ["ADMIN"]);

const STATS = {
    totalOwners: 5,
    totalVets: 3,
    totalPets: 12,
    totalAppointments: 20,
    appointmentsPerVet: [
        {vetUsername: "drsmith", count: 10},
        {vetUsername: "drjones", count: 7},
    ],
};

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
                <AdminDashboardPage/>
            </AuthContext.Provider>
        </MemoryRouter>
    );
}

describe("AdminDashboardPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders stat cards", async () => {
        // arrange
        (apiClient.getStats as any).mockResolvedValue(STATS);

        renderPage();

        // act + assert
        expect(await screen.findByText("5")).toBeInTheDocument();
        expect(screen.getByText("Owners")).toBeInTheDocument();
        expect(screen.getByText("3")).toBeInTheDocument();
        expect(screen.getByText("Vets")).toBeInTheDocument();
        expect(screen.getByText("12")).toBeInTheDocument();
        expect(screen.getByText("Pets")).toBeInTheDocument();
        expect(screen.getByText("20")).toBeInTheDocument();
        expect(screen.getAllByText("Appointments").length).toBeGreaterThanOrEqual(1);
    });

    it("renders appointments per vet table", async () => {
        // arrange
        (apiClient.getStats as any).mockResolvedValue(STATS);

        renderPage();

        // act + assert
        expect(await screen.findByText("drsmith")).toBeInTheDocument();
        expect(screen.getByText("drjones")).toBeInTheDocument();
        expect(screen.getByText("10")).toBeInTheDocument();
        expect(screen.getByText("7")).toBeInTheDocument();
    });

    it("shows loading state", async () => {
        // arrange
        let resolve!: (v: any) => void;
        (apiClient.getStats as any).mockReturnValue(new Promise(r => { resolve = r; }));

        renderPage();

        // act + assert
        expect(screen.getByText("Loading...")).toBeInTheDocument();

        resolve(STATS);
        await waitFor(() => {
            expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
        });
    });

    it("shows error on fetch failure", async () => {
        // arrange
        (apiClient.getStats as any).mockRejectedValue(new Error("Stats unavailable"));

        renderPage();

        // act + assert
        expect(await screen.findByText("Stats unavailable")).toBeInTheDocument();
    });

    it("shows empty state when no vet appointments", async () => {
        // arrange
        (apiClient.getStats as any).mockResolvedValue({
            ...STATS,
            appointmentsPerVet: [],
        });

        renderPage();

        // act + assert
        expect(await screen.findByText("No appointments recorded yet.")).toBeInTheDocument();
    });
});
