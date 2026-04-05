import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AppointmentsPage from "./AppointmentsPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listAppointments: vi.fn(),
        listPets: vi.fn(),
        listVets: vi.fn(),
        cancelAppointment: vi.fn(),
    }
}));

const PETS = [
    {id: 1, name: "Fluffy", species: "CAT", gender: "FEMALE"},
    {id: 2, name: "Rex", species: "DOG", gender: "MALE"},
];
const VETS = [
    {id: 10, username: "Dr. Smith"},
    {id: 20, username: "Dr. Jones"},
];
const APPOINTMENTS = [
    {id: 100, petId: 1, vetId: 10, startsAt: "2025-06-15T10:00:00"},
    {id: 200, petId: 2, vetId: 20, startsAt: "2025-07-20T14:30:00"},
];

function renderPage() {
    return render(
        <MemoryRouter>
            <AppointmentsPage/>
        </MemoryRouter>
    );
}

describe("AppointmentsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders page heading and book appointment link", async () => {
        (apiClient.listAppointments as any).mockResolvedValue([]);
        (apiClient.listPets as any).mockResolvedValue([]);
        (apiClient.listVets as any).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("My Appointments")).toBeInTheDocument();
        expect(screen.getByText("+ Book appointment")).toBeInTheDocument();
    });

    it("shows loading indicator then empty message when no appointments", async () => {
        (apiClient.listAppointments as any).mockResolvedValue([]);
        (apiClient.listPets as any).mockResolvedValue([]);
        (apiClient.listVets as any).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("Loading...")).toBeInTheDocument();
        expect(await screen.findByText("No appointments found.")).toBeInTheDocument();
    });

    it("shows appointments with resolved pet and vet names", async () => {
        (apiClient.listAppointments as any).mockResolvedValue(APPOINTMENTS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        renderPage();

        expect(await screen.findByText(/Fluffy/)).toBeInTheDocument();
        expect(screen.getByText(/Dr\. Smith/)).toBeInTheDocument();
        expect(screen.getByText(/Rex/)).toBeInTheDocument();
        expect(screen.getByText(/Dr\. Jones/)).toBeInTheDocument();
    });

    it("falls back to 'Pet #id' and 'Vet #id' when ids are not in lookup lists", async () => {
        const appointment = {id: 999, petId: 99, vetId: 88, startsAt: "2025-06-15T10:00:00"};
        (apiClient.listAppointments as any).mockResolvedValue([appointment]);
        (apiClient.listPets as any).mockResolvedValue([]);
        (apiClient.listVets as any).mockResolvedValue([]);

        renderPage();

        expect(await screen.findByText(/Pet #99/)).toBeInTheDocument();
        expect(screen.getByText(/Vet #88/)).toBeInTheDocument();
    });

    it("shows error message when fetching appointments fails", async () => {
        (apiClient.listAppointments as any).mockRejectedValue(new Error("Network error"));
        (apiClient.listPets as any).mockResolvedValue([]);
        (apiClient.listVets as any).mockResolvedValue([]);

        renderPage();

        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });

    it("cancel button calls cancelAppointment and removes the row", async () => {
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.cancelAppointment as any).mockResolvedValue(undefined);

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        await waitFor(() => {
            expect(screen.queryByText(/Fluffy/)).not.toBeInTheDocument();
        });
        expect(apiClient.cancelAppointment).toHaveBeenCalledWith(100);
    });

    it("shows error and keeps row when cancel fails", async () => {
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.cancelAppointment as any).mockRejectedValue(new Error("Cancel failed"));

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        expect(await screen.findByText("Cancel failed")).toBeInTheDocument();
        expect(screen.getByText(/Fluffy/)).toBeInTheDocument();
    });

    it("shows 'Cancelling…' on the button while cancel is in progress", async () => {
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        let resolveCancel!: () => void;
        (apiClient.cancelAppointment as any).mockReturnValue(
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
});