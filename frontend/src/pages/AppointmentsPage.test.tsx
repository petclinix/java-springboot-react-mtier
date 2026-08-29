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
        listAvailableSlots: vi.fn(),
        cancelAppointment: vi.fn(),
        rescheduleAppointment: vi.fn(),
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
    {id: 100, petId: 1, vetId: 10, locationId: 5, appointmentType: "CHECKUP", startsAt: "2025-06-15T10:00:00", status: "BOOKED"},
    {id: 200, petId: 2, vetId: 20, locationId: 6, appointmentType: "VACCINATION", startsAt: "2025-07-20T14:30:00", status: "CONFIRMED"},
];
const SLOTS = [
    {startsAt: "2025-09-01T09:00:00", endsAt: "2025-09-01T09:30:00"},
    {startsAt: "2025-09-01T11:00:00", endsAt: "2025-09-01T11:30:00"},
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
        const appointment = {id: 999, petId: 99, vetId: 88, startsAt: "2025-06-15T10:00:00", status: "BOOKED"};
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

    it("shows a status badge for each appointment", async () => {
        // arrange
        (apiClient.listAppointments as any).mockResolvedValue(APPOINTMENTS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("BOOKED")).toBeInTheDocument();
        expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    });

    it("shows the Reschedule button only for BOOKED or CONFIRMED appointments", async () => {
        // arrange
        const appointments = [
            ...APPOINTMENTS,
            {id: 300, petId: 1, vetId: 10, startsAt: "2025-08-01T09:00:00", status: "COMPLETED"},
        ];
        (apiClient.listAppointments as any).mockResolvedValue(appointments);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        // act
        renderPage();
        await screen.findByText("BOOKED");

        // assert
        expect(screen.getAllByRole("button", {name: /^reschedule$/i})).toHaveLength(2);
    });

    it("hides Cancel and Reschedule buttons for terminal statuses", async () => {
        // arrange
        const appointments = [
            {id: 300, petId: 1, vetId: 10, startsAt: "2025-08-01T09:00:00", status: "COMPLETED"},
            {id: 400, petId: 2, vetId: 20, startsAt: "2025-08-02T09:00:00", status: "CANCELLED"},
        ];
        (apiClient.listAppointments as any).mockResolvedValue(appointments);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        // act
        renderPage();
        await screen.findByText(/Fluffy/);

        // assert
        expect(screen.queryByRole("button", {name: /^reschedule$/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /^cancel$/i})).not.toBeInTheDocument();
    });

    it("clicking Reschedule opens an inline form with a date input", async () => {
        // arrange
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        // act
        renderPage();
        await screen.findByText(/Fluffy/);
        fireEvent.click(screen.getByRole("button", {name: /^reschedule$/i}));

        // assert
        expect(screen.getByRole("button", {name: /^save$/i})).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /^close$/i})).toBeInTheDocument();
    });

    it("fetches slots for the appointment's own location and appointment type once a date is chosen", async () => {
        // arrange
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);

        // act
        const {container} = renderPage();
        await screen.findByText(/Fluffy/);
        fireEvent.click(screen.getByRole("button", {name: /^reschedule$/i}));
        const dateInput = container.querySelector("input[type='date']")!;
        fireEvent.change(dateInput, {target: {value: "2025-09-01"}});

        // assert
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(5, "2025-09-01", "CHECKUP");
        });
        expect(await screen.findByText("11:00 AM")).toBeInTheDocument();
    });

    it("submitting a reschedule calls rescheduleAppointment with the selected slot's startsAt and updates the row", async () => {
        // arrange
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);
        const updated = {...APPOINTMENTS[0], startsAt: SLOTS[1].startsAt, status: "BOOKED"};
        (apiClient.rescheduleAppointment as any).mockResolvedValue(updated);

        // act
        const {container} = renderPage();
        await screen.findByText(/Fluffy/);
        fireEvent.click(screen.getByRole("button", {name: /^reschedule$/i}));
        const dateInput = container.querySelector("input[type='date']")!;
        fireEvent.change(dateInput, {target: {value: "2025-09-01"}});
        await screen.findByText("11:00 AM");
        fireEvent.click(screen.getByRole("button", {name: "11:00 AM"}));
        fireEvent.click(screen.getByRole("button", {name: /^save$/i}));

        // assert
        await waitFor(() => {
            expect(apiClient.rescheduleAppointment).toHaveBeenCalledWith(100, SLOTS[1].startsAt);
        });
        expect(await screen.findByText(new Date(updated.startsAt).toLocaleString())).toBeInTheDocument();
        // form closes after a successful save
        expect(screen.queryByRole("button", {name: /^save$/i})).not.toBeInTheDocument();
    });

    it("shows an inline error and keeps the form open with the selected slot when reschedule fails", async () => {
        // arrange
        (apiClient.listAppointments as any).mockResolvedValue([APPOINTMENTS[0]]);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);
        (apiClient.rescheduleAppointment as any).mockRejectedValue(new Error("Cannot reschedule: appointment starts too soon"));

        // act
        const {container} = renderPage();
        await screen.findByText(/Fluffy/);
        fireEvent.click(screen.getByRole("button", {name: /^reschedule$/i}));
        const dateInput = container.querySelector("input[type='date']")!;
        fireEvent.change(dateInput, {target: {value: "2025-09-01"}});
        await screen.findByText("11:00 AM");
        fireEvent.click(screen.getByRole("button", {name: "11:00 AM"}));
        fireEvent.click(screen.getByRole("button", {name: /^save$/i}));

        // assert
        expect(await screen.findByText("Cannot reschedule: appointment starts too soon")).toBeInTheDocument();
        // form stays open with the user's in-progress selection preserved
        expect(screen.getByRole("button", {name: /^save$/i})).toBeInTheDocument();
        expect((dateInput as HTMLInputElement).value).toBe("2025-09-01");
        expect(screen.getByText("11:00 AM")).toBeInTheDocument();
    });

    it("shows a badge with the appointment type next to the status badge", async () => {
        // arrange
        (apiClient.listAppointments as any).mockResolvedValue(APPOINTMENTS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listVets as any).mockResolvedValue(VETS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("CHECKUP")).toBeInTheDocument();
        expect(screen.getByText("VACCINATION")).toBeInTheDocument();
    });
});
