import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import AppointmentBookingPage from "./AppointmentBookingPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listVets: vi.fn(),
        listPets: vi.fn(),
        createAppointment: vi.fn(),
    }
}));

const VETS = [{id: 10, name: "Dr. Smith"}, {id: 20, name: "Dr. Jones"}];
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
        (apiClient.listVets as any).mockResolvedValue([]);
        (apiClient.listPets as any).mockResolvedValue([]);

        renderPage();

        expect(screen.getByText("Book an appointment")).toBeInTheDocument();
    });

    it("populates vet and pet selects after loading", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        renderPage();

        expect(await screen.findByText("Dr. Smith")).toBeInTheDocument();
        expect(screen.getByText("Dr. Jones")).toBeInTheDocument();
        expect(screen.getByText("Fluffy — CAT")).toBeInTheDocument();
        expect(screen.getByText("Rex — DOG")).toBeInTheDocument();
    });

    it("shows error when date is not provided", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        renderPage();

        await screen.findByText("Dr. Smith");

        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        expect(await screen.findByText("Please choose a date and time.")).toBeInTheDocument();
    });

    it("shows error when date is in the past", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        const {container} = renderPage();

        await screen.findByText("Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")!;
        fireEvent.change(dateInput, {target: {value: "2000-01-01T10:00"}});

        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        expect(await screen.findByText("Please choose a future date/time.")).toBeInTheDocument();
    });

    it("shows success message with appointment id after booking", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.createAppointment as any).mockResolvedValue({
            id: 42, vetId: 10, petId: 1, startsAt: "2099-12-31T10:00:00"
        });

        const {container} = renderPage();

        await screen.findByText("Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")!;
        fireEvent.change(dateInput, {target: {value: "2099-12-31T10:00"}});

        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        expect(await screen.findByText(/appointment created \(id: 42\)/i)).toBeInTheDocument();
        expect(apiClient.createAppointment).toHaveBeenCalledOnce();
    });

    it("resets date field to empty after successful booking", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.createAppointment as any).mockResolvedValue({id: 1});

        const {container} = renderPage();

        await screen.findByText("Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")! as HTMLInputElement;
        fireEvent.change(dateInput, {target: {value: "2099-12-31T10:00"}});
        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        await waitFor(() => {
            expect(dateInput.value).toBe("");
        });
    });

    it("shows error message when booking fails", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);
        (apiClient.createAppointment as any).mockRejectedValue(new Error("Server returned 500"));

        const {container} = renderPage();

        await screen.findByText("Dr. Smith");

        const dateInput = container.querySelector("input[type='datetime-local']")!;
        fireEvent.change(dateInput, {target: {value: "2099-12-31T10:00"}});

        fireEvent.click(screen.getByRole("button", {name: /book appointment/i}));

        expect(await screen.findByText("Server returned 500")).toBeInTheDocument();
    });

    it("prefill button sets date to tomorrow", async () => {
        (apiClient.listVets as any).mockResolvedValue(VETS);
        (apiClient.listPets as any).mockResolvedValue(PETS);

        const {container} = renderPage();

        await screen.findByText("Dr. Smith");

        fireEvent.click(screen.getByRole("button", {name: /prefill/i}));

        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const tomorrowDate = tomorrow.toISOString().substring(0, 10);

        const dateInput = container.querySelector("input[type='datetime-local']")! as HTMLInputElement;
        expect(dateInput.value).toMatch(new RegExp(`^${tomorrowDate}`));
        expect(dateInput.value).not.toBe("");
    });

    it("shows 'No vets available' and 'No pets available' when lists are empty", async () => {
        (apiClient.listVets as any).mockResolvedValue([]);
        (apiClient.listPets as any).mockResolvedValue([]);

        renderPage();

        expect(await screen.findByText("No vets available")).toBeInTheDocument();
        expect(screen.getByText("No pets available")).toBeInTheDocument();
    });
});