import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {vi} from "vitest";
import LocationsPage from "./LocationsPage";
import {apiClient} from "../client/ApiClient";
import type {Location} from "../client/dto/Location.ts";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listLocations: vi.fn(),
        retrieveLocations: vi.fn(),
        saveLocation: vi.fn(),
        deleteLocations: vi.fn(),
    }
}));

// Address fields render in this fixed order, before the weekly-periods/overrides
// sections, so they are always the first six <input> elements in the detail panel.
const NAME = 0;
const ZONE_ID = 1;

const LOCATIONS: Location[] = [
    {
        id: 1,
        name: "Downtown Clinic",
        zoneId: "Europe/Vienna",
        street: "Main St 1",
        postalCode: "1010",
        city: "Vienna",
        country: "Austria",
        weeklyPeriods: [
            {dayOfWeek: 1, startTime: "09:00", endTime: "17:00", sortOrder: 0},
        ],
        overrides: [
            {date: "2026-12-25", closed: true, reason: "Christmas"},
        ],
    },
    {
        id: 2,
        name: "Uptown Clinic",
        zoneId: "Europe/Vienna",
        street: "Second St 2",
        postalCode: "1020",
        city: "Vienna",
        country: "Austria",
        weeklyPeriods: [],
        overrides: [],
    },
];

function renderPage() {
    return render(<LocationsPage/>);
}

function addressInput(container: HTMLElement, index: number): HTMLInputElement {
    return container.querySelectorAll("input")[index] as HTMLInputElement;
}

describe("LocationsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("renders the page heading", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([]);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Locations")).toBeInTheDocument();
    });

    it("shows the fetched list of locations", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Downtown Clinic")).toBeInTheDocument();
        expect(screen.getByText("Uptown Clinic")).toBeInTheDocument();
    });

    it("shows the empty state when there are no locations", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([]);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("No locations yet.")).toBeInTheDocument();
    });

    it("shows an error message when listLocations fails", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("boom"));

        // act
        renderPage();

        // assert
        expect(await screen.findByText("boom")).toBeInTheDocument();
    });

    it("clicking New shows a blank form in edit mode", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([]);
        const {container} = renderPage();
        await screen.findByText("No locations yet.");

        // act
        fireEvent.click(screen.getByRole("button", {name: "New"}));

        // assert
        expect(screen.getByText("New location")).toBeInTheDocument();
        const nameInput = addressInput(container, NAME);
        expect(nameInput).not.toBeDisabled();
        expect(nameInput.value).toBe("");
        expect(addressInput(container, ZONE_ID).value).toBe("Europe/Vienna");
    });

    it("clicking a location name loads it read-only", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[0]);
        const {container} = renderPage();
        await screen.findByText("Downtown Clinic");

        // act
        fireEvent.click(screen.getByText("Downtown Clinic"));

        // assert
        await waitFor(() => expect(apiClient.retrieveLocations).toHaveBeenCalledWith(1));
        expect(await screen.findByText("#1 Downtown Clinic")).toBeInTheDocument();
        const nameInput = addressInput(container, NAME);
        expect(nameInput).toBeDisabled();
        expect(nameInput.value).toBe("Downtown Clinic");
    });

    it("clicking Open loads the location read-only", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[1]);
        renderPage();
        await screen.findByText("Downtown Clinic");

        // act
        fireEvent.click(screen.getAllByRole("button", {name: "Open"})[1]);

        // assert
        await waitFor(() => expect(apiClient.retrieveLocations).toHaveBeenCalledWith(2));
        expect(await screen.findByText("#2 Uptown Clinic")).toBeInTheDocument();
    });

    it("clicking Edit enables the inputs with the current values", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[0]);
        const {container} = renderPage();
        await screen.findByText("Downtown Clinic");
        fireEvent.click(screen.getByText("Downtown Clinic"));
        await screen.findByText("#1 Downtown Clinic");

        // act
        fireEvent.click(screen.getAllByRole("button", {name: "Edit"})[0]);

        // assert
        const nameInput = addressInput(container, NAME);
        expect(nameInput).not.toBeDisabled();
        expect(nameInput.value).toBe("Downtown Clinic");
    });

    describe("weekly periods", () => {
        async function openNewLocationForm() {
            (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([]);
            renderPage();
            await screen.findByText("No locations yet.");
            fireEvent.click(screen.getByRole("button", {name: "New"}));
            await screen.findByText("New location");
        }

        it("adding a period appends a row with default day/time", async () => {
            // arrange
            await openNewLocationForm();
            expect(screen.getByText("No weekly periods")).toBeInTheDocument();

            // act
            fireEvent.click(screen.getByRole("button", {name: "Add period"}));

            // assert
            const daySelect = screen.getByRole("combobox") as HTMLSelectElement;
            expect(daySelect.value).toBe("1");
            const timeInputs = screen.getAllByDisplayValue(/^(09:00|17:00)$/);
            expect(timeInputs).toHaveLength(2);
        });

        it("changing a period's day/start/end updates its values", async () => {
            // arrange
            await openNewLocationForm();
            fireEvent.click(screen.getByRole("button", {name: "Add period"}));
            const daySelect = screen.getByRole("combobox") as HTMLSelectElement;
            const [startInput, endInput] = screen.getAllByDisplayValue(/^(09:00|17:00)$/) as HTMLInputElement[];

            // act
            fireEvent.change(daySelect, {target: {value: "3"}});
            fireEvent.change(startInput, {target: {value: "10:00"}});
            fireEvent.change(endInput, {target: {value: "18:00"}});

            // assert
            expect(daySelect.value).toBe("3");
            expect(startInput.value).toBe("10:00");
            expect(endInput.value).toBe("18:00");
        });

        it("removing a period removes its row", async () => {
            // arrange
            await openNewLocationForm();
            fireEvent.click(screen.getByRole("button", {name: "Add period"}));
            fireEvent.click(screen.getByRole("button", {name: "Add period"}));
            expect(screen.getAllByRole("combobox")).toHaveLength(2);

            // act
            fireEvent.click(screen.getAllByRole("button", {name: "Remove"})[0]);

            // assert
            expect(screen.getAllByRole("combobox")).toHaveLength(1);
        });
    });

    describe("overrides", () => {
        async function openNewLocationForm() {
            (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([]);
            const result = renderPage();
            await screen.findByText("No locations yet.");
            fireEvent.click(screen.getByRole("button", {name: "New"}));
            await screen.findByText("New location");
            return result;
        }

        it("adding an override defaults to closed with no time inputs", async () => {
            // arrange
            const {container} = await openNewLocationForm();
            expect(screen.getByText("No overrides")).toBeInTheDocument();

            // act
            fireEvent.click(screen.getByRole("button", {name: "Add override"}));

            // assert
            const closedCheckbox = screen.getByLabelText("Closed") as HTMLInputElement;
            expect(closedCheckbox.checked).toBe(true);
            expect(screen.getByPlaceholderText("reason")).toBeInTheDocument();
            expect(container.querySelectorAll("input[type='time']")).toHaveLength(0);
        });

        it("unchecking Closed reveals the open/close time inputs", async () => {
            // arrange
            const {container} = await openNewLocationForm();
            fireEvent.click(screen.getByRole("button", {name: "Add override"}));
            const closedCheckbox = screen.getByLabelText("Closed") as HTMLInputElement;

            // act
            fireEvent.click(closedCheckbox);

            // assert
            await waitFor(() => expect(closedCheckbox.checked).toBe(false));
            expect(container.querySelectorAll("input[type='time']")).toHaveLength(2);
        });

        it("removing an override removes its row", async () => {
            // arrange
            await openNewLocationForm();
            fireEvent.click(screen.getByRole("button", {name: "Add override"}));
            expect(screen.getByPlaceholderText("reason")).toBeInTheDocument();

            // act
            fireEvent.click(screen.getByRole("button", {name: "Remove"}));

            // assert
            expect(screen.queryByPlaceholderText("reason")).not.toBeInTheDocument();
            expect(screen.getByText("No overrides")).toBeInTheDocument();
        });
    });

    it("saving posts the edited fields including weekly periods and overrides, then exits edit mode and refreshes", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([]);
        const saved: Location = {
            id: 5,
            name: "New Clinic",
            zoneId: "Europe/Vienna",
            street: "",
            postalCode: "",
            city: "",
            country: "",
            weeklyPeriods: [{dayOfWeek: 1, startTime: "09:00", endTime: "17:00", sortOrder: 0}],
            overrides: [{date: "2026-12-25", closed: true, reason: "Xmas"}],
        };
        (apiClient.saveLocation as ReturnType<typeof vi.fn>).mockResolvedValue(saved);
        const {container} = renderPage();
        await screen.findByText("No locations yet.");
        fireEvent.click(screen.getByRole("button", {name: "New"}));
        await screen.findByText("New location");

        fireEvent.change(addressInput(container, NAME), {target: {value: "New Clinic"}});
        fireEvent.click(screen.getByRole("button", {name: "Add period"}));
        fireEvent.click(screen.getByRole("button", {name: "Add override"}));
        fireEvent.change(screen.getByPlaceholderText("reason"), {target: {value: "Xmas"}});

        // act
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([saved]);
        fireEvent.click(screen.getByRole("button", {name: "Save"}));

        // assert
        await waitFor(() => expect(apiClient.saveLocation).toHaveBeenCalledWith(
            expect.objectContaining({
                name: "New Clinic",
                weeklyPeriods: expect.arrayContaining([
                    expect.objectContaining({dayOfWeek: 1, startTime: "09:00", endTime: "17:00"}),
                ]),
                overrides: expect.arrayContaining([
                    expect.objectContaining({reason: "Xmas", closed: true}),
                ]),
            })
        ));
        await waitFor(() => expect(apiClient.listLocations).toHaveBeenCalledTimes(2));
        expect(await screen.findByText("#5 New Clinic")).toBeInTheDocument();
        expect(addressInput(container, NAME)).toBeDisabled();
    });

    it("cancel while editing an existing location reloads it, discarding in-progress edits", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[0]);
        const {container} = renderPage();
        await screen.findByText("Downtown Clinic");
        fireEvent.click(screen.getByText("Downtown Clinic"));
        await screen.findByText("#1 Downtown Clinic");
        fireEvent.click(screen.getAllByRole("button", {name: "Edit"})[0]);

        fireEvent.change(addressInput(container, NAME), {target: {value: "Changed Name"}});
        expect(addressInput(container, NAME).value).toBe("Changed Name");
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockClear();

        // act — the bottom-panel Cancel button (in the Save/Cancel/Close row) reloads via retrieveLocations,
        // discarding edits, as opposed to the top-panel Cancel which just clears the selection.
        const cancelButtons = screen.getAllByRole("button", {name: "Cancel"});
        fireEvent.click(cancelButtons[cancelButtons.length - 1]);

        // assert
        await waitFor(() => expect(apiClient.retrieveLocations).toHaveBeenCalledWith(1));
        expect(await screen.findByText("#1 Downtown Clinic")).toBeInTheDocument();
        expect(addressInput(container, NAME).value).toBe("Downtown Clinic");
    });

    it("delete with confirmation calls deleteLocations, clears selection and refreshes the list", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[0]);
        (apiClient.deleteLocations as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
        window.confirm = vi.fn().mockReturnValue(true);
        renderPage();
        await screen.findByText("Downtown Clinic");
        fireEvent.click(screen.getByText("Downtown Clinic"));
        await screen.findByText("#1 Downtown Clinic");

        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue([LOCATIONS[1]]);

        // act
        fireEvent.click(screen.getByRole("button", {name: "Delete"}));

        // assert
        await waitFor(() => expect(apiClient.deleteLocations).toHaveBeenCalledWith(1));
        await waitFor(() => expect(screen.queryByText("#1 Downtown Clinic")).not.toBeInTheDocument());
        expect(screen.getByText("Select a location to view/edit or click New.")).toBeInTheDocument();
        expect(await screen.findByText("Uptown Clinic")).toBeInTheDocument();
    });

    it("declining delete confirmation does not call deleteLocations and keeps the selection", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[0]);
        window.confirm = vi.fn().mockReturnValue(false);
        renderPage();
        await screen.findByText("Downtown Clinic");
        fireEvent.click(screen.getByText("Downtown Clinic"));
        await screen.findByText("#1 Downtown Clinic");

        // act
        fireEvent.click(screen.getByRole("button", {name: "Delete"}));

        // assert
        expect(apiClient.deleteLocations).not.toHaveBeenCalled();
        expect(screen.getByText("#1 Downtown Clinic")).toBeInTheDocument();
    });

    it("Close clears the selection back to the placeholder", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        (apiClient.retrieveLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS[0]);
        renderPage();
        await screen.findByText("Downtown Clinic");
        fireEvent.click(screen.getByText("Downtown Clinic"));
        await screen.findByText("#1 Downtown Clinic");

        // act
        fireEvent.click(screen.getByRole("button", {name: "Close"}));

        // assert
        expect(screen.getByText("Select a location to view/edit or click New.")).toBeInTheDocument();
        expect(screen.queryByText("#1 Downtown Clinic")).not.toBeInTheDocument();
    });

    it("Refresh re-calls listLocations", async () => {
        // arrange
        (apiClient.listLocations as ReturnType<typeof vi.fn>).mockResolvedValue(LOCATIONS);
        renderPage();
        await screen.findByText("Downtown Clinic");
        expect(apiClient.listLocations).toHaveBeenCalledTimes(1);

        // act
        fireEvent.click(screen.getByRole("button", {name: "Refresh"}));

        // assert
        await waitFor(() => expect(apiClient.listLocations).toHaveBeenCalledTimes(2));
    });
});
