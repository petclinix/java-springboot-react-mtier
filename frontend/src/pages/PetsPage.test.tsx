import {fireEvent, render, screen} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import PetsPage from "./PetsPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listPets: vi.fn(),
        createPet: vi.fn(),
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

const PETS = [
    {id: 1, name: "Fluffy", species: "CAT", gender: "FEMALE", breed: "Siamese", birthDate: "2020-01-01"},
    {id: 2, name: "Rex", species: "DOG", gender: "MALE", breed: null, birthDate: null},
];

function renderPage() {
    return render(
        <MemoryRouter>
            <PetsPage/>
        </MemoryRouter>
    );
}

describe("PetsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockNavigate.mockReset();
    });

    it("view visits button navigates to pet visits page", async () => {
        // arrange
        (apiClient.listPets as any).mockResolvedValue(PETS);

        renderPage();
        await screen.findByText("Fluffy");

        // act
        const viewVisitsButtons = screen.getAllByRole("button", {name: /view visits/i});
        fireEvent.click(viewVisitsButtons[0]);

        // assert
        expect(mockNavigate).toHaveBeenCalledWith("/pets/1/visits");
    });

    it("renders breed for a pet that has one and skips it for a pet that doesn't", async () => {
        // arrange
        (apiClient.listPets as any).mockResolvedValue(PETS);

        // act
        renderPage();
        await screen.findByText("Fluffy");

        // assert
        expect(screen.getByText("Siamese")).toBeInTheDocument();
        expect(screen.getByText("Rex")).toBeInTheDocument();
    });

    it("submits a new pet including breed and prepends it to the list", async () => {
        // arrange
        (apiClient.listPets as any).mockResolvedValue([]);
        (apiClient.createPet as any).mockResolvedValue({
            id: 3, name: "Buddy", species: "DOG", gender: "MALE", breed: "Labrador", birthDate: "2021-05-05",
        });

        const {container} = renderPage();
        await screen.findByText("No pets found.");

        const nameInput = container.querySelectorAll("input")[0] as HTMLInputElement;
        const breedInput = container.querySelectorAll("input")[1] as HTMLInputElement;

        // act
        fireEvent.change(nameInput, {target: {value: "Buddy"}});
        fireEvent.change(breedInput, {target: {value: "Labrador"}});
        fireEvent.click(screen.getByRole("button", {name: /add pet/i}));

        // assert
        await screen.findByText("Buddy");
        expect(apiClient.createPet).toHaveBeenCalledWith(
            expect.objectContaining({name: "Buddy", breed: "Labrador"})
        );
        expect(screen.getByText("Labrador")).toBeInTheDocument();
    });
});
