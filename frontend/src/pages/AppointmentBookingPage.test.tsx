import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AppointmentBookingPage from "./AppointmentBookingPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listBookableLocations: vi.fn(),
        listPets: vi.fn(),
        listAvailableSlots: vi.fn(),
        createAppointment: vi.fn(),
    }
}));

const LOCATIONS = [
    {id: 10, name: "Clinic North", vetUsername: "Dr. Smith", zoneId: "Europe/Vienna"},
    {id: 20, name: "Clinic South", vetUsername: "Dr. Jones", zoneId: "Europe/Vienna"},
];
const PETS = [
    {id: 1, name: "Fluffy", species: "CAT", gender: "FEMALE"},
    {id: 2, name: "Rex", species: "DOG", gender: "MALE"},
];
const SLOTS = [
    {startsAt: "2099-12-31T09:00:00", endsAt: "2099-12-31T09:30:00"},
    {startsAt: "2099-12-31T10:00:00", endsAt: "2099-12-31T10:30:00"},
];

function renderPage() {
    return render(
        <MemoryRouter>
            <AppointmentBookingPage/>
        </MemoryRouter>
    );
}

async function selectDate(container: HTMLElement, value: string) {
    const dateInput = container.querySelector("input[type='date']")!;
    fireEvent.change(dateInput, {target: {value}});
}

describe("AppointmentBookingPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders heading", () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue([]);
        (apiClient.listPets as any).mockResolvedValue([]);

        // act
        renderPage();

        // assert
        expect(screen.getByText("Book an appointment")).toBeInTheDocument();
    });

    it("populates location and pet selects after loading", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Clinic North — Dr. Smith")).toBeInTheDocument();
        expect(screen.getByText("Clinic South — Dr. Jones")).toBeInTheDocument();
        expect(screen.getByText("Fluffy — CAT")).toBeInTheDocument();
        expect(screen.getByText("Rex — DOG")).toBeInTheDocument();
    });

    it("renders an appointment type selector with all five values, defaulting to the first", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        // act
        renderPage();
        await screen.findByText("Clinic North — Dr. Smith");

        // assert
        expect(screen.getByText("VACCINATION")).toBeInTheDocument();
        expect(screen.getByText("FOLLOW_UP")).toBeInTheDocument();
        expect(screen.getByText("CHECKUP")).toBeInTheDocument();
        expect(screen.getByText("EMERGENCY")).toBeInTheDocument();
        expect(screen.getByText("SURGERY")).toBeInTheDocument();
    });

    it("shows error when no slot has been chosen", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        renderPage();
        await screen.findByText("Clinic North — Dr. Smith");

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText("Please choose a date.")).toBeInTheDocument();
    });

    it("fetches slots once location, appointment type and date are all chosen, and re-fetches when the date changes", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");

        // act
        await selectDate(container, "2099-12-31");

        // assert
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(10, "2099-12-31", "VACCINATION");
        });
        expect(await screen.findByText("09:00 AM")).toBeInTheDocument();

        // act: change date again -> re-fetch
        await selectDate(container, "2100-01-01");

        // assert
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(10, "2100-01-01", "VACCINATION");
        });
    });

    it("re-fetches slots when the appointment type changes", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");
        await selectDate(container, "2099-12-31");
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(10, "2099-12-31", "VACCINATION");
        });

        // act
        const typeSelect = screen.getByDisplayValue("VACCINATION");
        fireEvent.change(typeSelect, {target: {value: "SURGERY"}});

        // assert
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(10, "2099-12-31", "SURGERY");
        });
        await screen.findByText("09:00 AM");
    });

    it("re-fetches slots when the location changes", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");
        await selectDate(container, "2099-12-31");
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(10, "2099-12-31", "VACCINATION");
        });

        // act
        const locationSelect = screen.getByDisplayValue("Clinic North — Dr. Smith");
        fireEvent.change(locationSelect, {target: {value: "20"}});

        // assert
        await waitFor(() => {
            expect(apiClient.listAvailableSlots).toHaveBeenCalledWith(20, "2099-12-31", "VACCINATION");
        });
        await screen.findByText("09:00 AM");
    });

    it("shows the empty-state message when no slots are available for the chosen day", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue([]);

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");

        // act
        await selectDate(container, "2099-12-31");

        // assert
        expect(await screen.findByText("No available slots for this day — try another date.")).toBeInTheDocument();
    });

    it("shows an error when fetching slots fails", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockRejectedValue(new Error("Failed to load slots"));

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");

        // act
        await selectDate(container, "2099-12-31");

        // assert
        expect(await screen.findByText("Failed to load slots")).toBeInTheDocument();
    });

    it("selecting a slot and submitting calls createAppointment with the slot's startsAt and the chosen appointmentType", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);
        (apiClient.createAppointment as any).mockResolvedValue({
            id: 42, vetId: 10, petId: 1, startsAt: SLOTS[1].startsAt
        });

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");
        await selectDate(container, "2099-12-31");
        await screen.findByText("10:00 AM");

        // act
        fireEvent.click(screen.getByRole("button", {name: "10:00 AM"}));
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText(/appointment created \(id: 42\)/i)).toBeInTheDocument();
        expect(apiClient.createAppointment).toHaveBeenCalledTimes(1);
        expect(apiClient.createAppointment).toHaveBeenCalledWith({
            locationId: 10,
            petId: 1,
            appointmentType: "VACCINATION",
            startsAt: SLOTS[1].startsAt,
        });
    });

    it("shows error message when booking fails", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.listAvailableSlots as any).mockResolvedValue(SLOTS);
        (apiClient.createAppointment as any).mockRejectedValue(new Error("Server returned 500"));

        const {container} = renderPage();
        await screen.findByText("Clinic North — Dr. Smith");
        await selectDate(container, "2099-12-31");
        await screen.findByText("09:00 AM");
        fireEvent.click(screen.getByRole("button", {name: "09:00 AM"}));

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText("Server returned 500")).toBeInTheDocument();
    });

    it("shows 'No locations available' and 'No pets available' when lists are empty", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue([]);
        (apiClient.listPets as any).mockResolvedValue([]);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("No locations available")).toBeInTheDocument();
        expect(screen.getByText("No pets available")).toBeInTheDocument();
    });
});
