import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import VetAppointmentsPage from "./VetAppointmentsPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listVetAppointments: vi.fn(),
        cancelVetAppointment: vi.fn(),
    }
}));

const APPOINTMENTS = [
    {id: 1, petId: 10, petName: "Fluffy", ownerUsername: "alice", startsAt: "2025-06-15T10:00:00"},
    {id: 2, petId: 20, petName: "Rex",    ownerUsername: "bob",   startsAt: "2025-07-20T14:30:00"},
];

function renderPage() {
    return render(
        <MemoryRouter>
            <VetAppointmentsPage/>
        </MemoryRouter>
    );
}

describe("VetAppointmentsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders heading and refresh button", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("My Appointments")).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /refresh/i})).toBeInTheDocument();
    });

    it("shows loading indicator then empty message when no appointments", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("Loading...")).toBeInTheDocument();
        expect(await screen.findByText("No appointments found.")).toBeInTheDocument();
    });

    it("shows appointments with pet name and owner username", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue(APPOINTMENTS);

        renderPage();

        expect(await screen.findByText(/Fluffy/)).toBeInTheDocument();
        expect(screen.getByText(/alice/)).toBeInTheDocument();
        expect(screen.getByText(/Rex/)).toBeInTheDocument();
        expect(screen.getByText(/bob/)).toBeInTheDocument();
    });

    it("shows error message when fetch fails", async () => {
        (apiClient.listVetAppointments as any).mockRejectedValue(new Error("Network error"));

        renderPage();

        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });

    it("cancel removes the appointment row on success", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.cancelVetAppointment as any).mockResolvedValue(undefined);

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        await waitFor(() => {
            expect(screen.queryByText(/Fluffy/)).not.toBeInTheDocument();
        });
        expect(apiClient.cancelVetAppointment).toHaveBeenCalledWith(1);
    });

    it("shows error and keeps row when cancel fails", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.cancelVetAppointment as any).mockRejectedValue(new Error("Cancel failed"));

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        expect(await screen.findByText("Cancel failed")).toBeInTheDocument();
        expect(screen.getByText(/Fluffy/)).toBeInTheDocument();
    });

    it("shows 'Cancelling…' on the button while cancel is in progress", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);

        let resolveCancel!: () => void;
        (apiClient.cancelVetAppointment as any).mockReturnValue(
            new Promise<void>(resolve => { resolveCancel = resolve; })
        );

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        expect(await screen.findByRole("button", {name: /cancelling/i})).toBeDisabled();

        resolveCancel();
        await waitFor(() => {
            expect(screen.queryByText(/Fluffy/)).not.toBeInTheDocument();
        });
    });

    it("refresh button re-fetches appointments", async () => {
        (apiClient.listVetAppointments as any).mockResolvedValue([]);

        renderPage();

        await screen.findByText("No appointments found.");

        (apiClient.listVetAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        fireEvent.click(screen.getByRole("button", {name: /refresh/i}));

        expect(await screen.findByText(/Fluffy/)).toBeInTheDocument();
        expect(apiClient.listVetAppointments).toHaveBeenCalledTimes(2);
    });
});
