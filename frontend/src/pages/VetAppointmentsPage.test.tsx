import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import VetAppointmentsPage from "./VetAppointmentsPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listVetAppointments: vi.fn(),
        cancelVetAppointment: vi.fn(),
        confirmVetAppointment: vi.fn(),
        markVetAppointmentNoShow: vi.fn(),
    }
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
    const actual = await vi.importActual("react-router-dom");
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const APPOINTMENTS = [
    {id: 1, petId: 10, petName: "Fluffy", ownerUsername: "alice", startsAt: "2025-06-15T10:00:00", status: "BOOKED"},
    {id: 2, petId: 20, petName: "Rex",    ownerUsername: "bob",   startsAt: "2025-07-20T14:30:00", status: "CONFIRMED"},
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
        mockNavigate.mockReset();
    });

    it("renders heading and refresh button", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("My Appointments")).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /refresh/i})).toBeInTheDocument();
    });

    it("shows loading indicator then empty message when no appointments", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("Loading...")).toBeInTheDocument();
        expect(await screen.findByText("No appointments found.")).toBeInTheDocument();
    });

    it("shows appointments with pet name and owner username", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue(APPOINTMENTS);

        renderPage();

        expect(await screen.findByText(/Fluffy/)).toBeInTheDocument();
        expect(screen.getByText(/alice/)).toBeInTheDocument();
        expect(screen.getByText(/Rex/)).toBeInTheDocument();
        expect(screen.getByText(/bob/)).toBeInTheDocument();
    });

    it("shows error message when fetch fails", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("Network error"));

        renderPage();

        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });

    it("cancel removes the appointment row on success", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.cancelVetAppointment as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        await waitFor(() => {
            expect(screen.queryByText(/Fluffy/)).not.toBeInTheDocument();
        });
        expect(apiClient.cancelVetAppointment).toHaveBeenCalledWith(1);
    });

    it("shows error and keeps row when cancel fails", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.cancelVetAppointment as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("Cancel failed"));

        renderPage();

        const cancelBtn = await screen.findByRole("button", {name: /^cancel$/i});
        fireEvent.click(cancelBtn);

        expect(await screen.findByText("Cancel failed")).toBeInTheDocument();
        expect(screen.getByText(/Fluffy/)).toBeInTheDocument();
    });

    it("shows 'Cancelling…' on the button while cancel is in progress", async () => {
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);

        let resolveCancel!: () => void;
        (apiClient.cancelVetAppointment as ReturnType<typeof vi.fn>).mockReturnValue(
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
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([]);

        renderPage();

        await screen.findByText("No appointments found.");

        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);
        fireEvent.click(screen.getByRole("button", {name: /refresh/i}));

        expect(await screen.findByText(/Fluffy/)).toBeInTheDocument();
        expect(apiClient.listVetAppointments).toHaveBeenCalledTimes(2);
    });

    it("visit button navigates to visit page for a CONFIRMED appointment", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[1]]);

        renderPage();
        await screen.findByText(/Rex/);

        // act
        fireEvent.click(screen.getByRole("button", {name: /^visit$/i}));

        // assert
        expect(mockNavigate).toHaveBeenCalledWith(`/appointments/vet/visit/${APPOINTMENTS[1].id}`);
    });

    it("shows a status badge for each appointment", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue(APPOINTMENTS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("BOOKED")).toBeInTheDocument();
        expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    });

    it("shows a Confirm button only for a BOOKED appointment", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue(APPOINTMENTS);

        // act
        renderPage();
        await screen.findByText(/Fluffy/);

        // assert
        expect(screen.getAllByRole("button", {name: /^confirm$/i})).toHaveLength(1);
    });

    it("does not show Visit button for a BOOKED appointment", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);

        // act
        renderPage();
        await screen.findByText(/Fluffy/);

        // assert
        expect(screen.queryByRole("button", {name: /^visit$/i})).not.toBeInTheDocument();
    });

    it("confirming a BOOKED appointment calls confirmVetAppointment and updates status to CONFIRMED", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.confirmVetAppointment as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);

        // act
        renderPage();
        await screen.findByText(/Fluffy/);
        fireEvent.click(screen.getByRole("button", {name: /^confirm$/i}));

        // assert
        await waitFor(() => {
            expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
        });
        expect(apiClient.confirmVetAppointment).toHaveBeenCalledWith(1);
        // Confirm button disappears, Mark no-show and Visit appear now
        expect(screen.queryByRole("button", {name: /^confirm$/i})).not.toBeInTheDocument();
        expect(screen.getByRole("button", {name: /mark no-show/i})).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /^visit$/i})).toBeInTheDocument();
    });

    it("shows an error and keeps status BOOKED when confirm fails", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.confirmVetAppointment as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("Cannot confirm: appointment is not BOOKED"));

        // act
        renderPage();
        await screen.findByText(/Fluffy/);
        fireEvent.click(screen.getByRole("button", {name: /^confirm$/i}));

        // assert
        expect(await screen.findByText("Cannot confirm: appointment is not BOOKED")).toBeInTheDocument();
        expect(screen.getByText("BOOKED")).toBeInTheDocument();
    });

    it("shows a Mark no-show button only for a CONFIRMED appointment", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue(APPOINTMENTS);

        // act
        renderPage();
        await screen.findByText(/Rex/);

        // assert
        expect(screen.getAllByRole("button", {name: /mark no-show/i})).toHaveLength(1);
    });

    it("marking a CONFIRMED appointment as no-show calls markVetAppointmentNoShow and updates status", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[1]]);
        (apiClient.markVetAppointmentNoShow as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);

        // act
        renderPage();
        await screen.findByText(/Rex/);
        fireEvent.click(screen.getByRole("button", {name: /mark no-show/i}));

        // assert
        await waitFor(() => {
            expect(screen.getByText("NO_SHOW")).toBeInTheDocument();
        });
        expect(apiClient.markVetAppointmentNoShow).toHaveBeenCalledWith(2);
        // Terminal status: no action buttons remain
        expect(screen.queryByRole("button", {name: /mark no-show/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /^visit$/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /^cancel$/i})).not.toBeInTheDocument();
    });

    it("shows an error when marking no-show fails", async () => {
        // arrange
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([APPOINTMENTS[1]]);
        (apiClient.markVetAppointmentNoShow as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("Cannot mark no-show: appointment is not CONFIRMED"));

        // act
        renderPage();
        await screen.findByText(/Rex/);
        fireEvent.click(screen.getByRole("button", {name: /mark no-show/i}));

        // assert
        expect(await screen.findByText("Cannot mark no-show: appointment is not CONFIRMED")).toBeInTheDocument();
        expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    });

    it("does not show Confirm, Mark no-show, Visit or Cancel buttons for terminal statuses", async () => {
        // arrange
        const terminal = [
            {id: 3, petId: 30, petName: "Milo", ownerUsername: "carol", startsAt: "2025-08-01T09:00:00", status: "COMPLETED"},
            {id: 4, petId: 40, petName: "Coco", ownerUsername: "dave", startsAt: "2025-08-02T09:00:00", status: "CANCELLED"},
            {id: 5, petId: 50, petName: "Bella", ownerUsername: "erin", startsAt: "2025-08-03T09:00:00", status: "NO_SHOW"},
        ];
        (apiClient.listVetAppointments as ReturnType<typeof vi.fn>).mockResolvedValue(terminal);

        // act
        renderPage();
        await screen.findByText(/Milo/);

        // assert
        expect(screen.queryByRole("button", {name: /^confirm$/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /mark no-show/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /^visit$/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /^cancel$/i})).not.toBeInTheDocument();
    });
});
