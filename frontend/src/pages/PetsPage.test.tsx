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
    {id: 1, name: "Fluffy", species: "CAT", gender: "FEMALE", birthDate: "2020-01-01"},
    {id: 2, name: "Rex", species: "DOG", gender: "MALE", birthDate: null},
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
});
