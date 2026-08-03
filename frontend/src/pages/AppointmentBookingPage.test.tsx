import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AppointmentBookingPage from "./AppointmentBookingPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listBookableLocations: vi.fn(),
        listPets: vi.fn(),
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

function renderPage() {
    return render(
        <MemoryRouter>
            <AppointmentBookingPage/>
        </MemoryRouter>
    );
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

    it("shows error when date is not provided", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        renderPage();

        await screen.findByText("Clinic North — Dr. Smith");

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText("Please choose a date and time.")).toBeInTheDocument();
    });

    it("shows error when date is in the past", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        const {container} = renderPage();

        await screen.findByText("Clinic North — Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")!;
        fireEvent.change(dateInput, {target: {value: "2000-01-01T10:00"}});

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText("Please choose a future date/time.")).toBeInTheDocument();
    });

    it("shows success message with appointment id after booking", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.createAppointment as any).mockResolvedValue({
            id: 42, vetId: 10, petId: 1, startsAt: "2099-12-31T10:00:00"
        });

        const {container} = renderPage();

        await screen.findByText("Clinic North — Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")!;
        fireEvent.change(dateInput, {target: {value: "2099-12-31T10:00"}});

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText(/appointment created \(id: 42\)/i)).toBeInTheDocument();
        expect(apiClient.createAppointment).toHaveBeenCalledTimes(1);
        expect(apiClient.createAppointment).toHaveBeenCalledWith(expect.objectContaining({ locationId: 10 }));
    });

    it("resets date field to empty after successful booking", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.createAppointment as any).mockResolvedValue({id: 1});

        const {container} = renderPage();

        await screen.findByText("Clinic North — Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")! as HTMLInputElement;
        fireEvent.change(dateInput, {target: {value: "2099-12-31T10:00"}});

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        await waitFor(() => {
            expect(dateInput.value).toBe("");
        });
    });

    it("shows error message when booking fails", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.createAppointment as any).mockRejectedValue(new Error("Server returned 500"));

        const {container} = renderPage();

        await screen.findByText("Clinic North — Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")!;
        fireEvent.change(dateInput, {target: {value: "2099-12-31T10:00"}});

        // act
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        // assert
        expect(await screen.findByText("Server returned 500")).toBeInTheDocument();
    });

    it("prefill button sets date to tomorrow", async () => {
        // arrange
        (apiClient.listBookableLocations as any).mockResolvedValue(LOCATIONS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        const {container} = renderPage();

        await screen.findByText("Clinic North — Dr. Smith");

        // act
        fireEvent.click(screen.getByRole("button", {name: /prefill/i}));

        // assert
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const tomorrowDate = tomorrow.toISOString().substring(0, 10);

        const dateInput = container.querySelector("input[type='datetime-local']")! as HTMLInputElement;
        expect(dateInput.value).toMatch(new RegExp(`^${tomorrowDate}`));
        expect(dateInput.value).not.toBe("");
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
