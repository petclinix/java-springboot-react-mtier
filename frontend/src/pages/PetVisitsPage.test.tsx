import {fireEvent, render, screen} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import {vi} from "vitest";
import PetVisitsPage from "./PetVisitsPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listPetVisits: vi.fn(),
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

const VISITS = [
    {id: 1, vetUsername: "drsmith", startsAt: "2025-06-15T10:00:00", ownerSummary: "Pet was calm", vaccination: "Rabies"},
    {id: 2, vetUsername: "drjones", startsAt: "2025-07-20T14:30:00", ownerSummary: "Follow up needed", vaccination: "Distemper"},
];

function renderPage(petId = "42") {
    return render(
        <MemoryRouter initialEntries={[`/pets/${petId}/visits`]}>
            <Routes>
                <Route path="/pets/:petId/visits" element={<PetVisitsPage/>}/>
            </Routes>
        </MemoryRouter>
    );
}

describe("PetVisitsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockNavigate.mockReset();
    });

    it("renders visit list", async () => {
        // arrange
        (apiClient.listPetVisits as any).mockResolvedValue(VISITS);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("drsmith")).toBeInTheDocument();
        expect(screen.getByText("Pet was calm")).toBeInTheDocument();
        expect(screen.getByText("Rabies")).toBeInTheDocument();
        expect(screen.getByText("drjones")).toBeInTheDocument();
        expect(screen.getByText("Follow up needed")).toBeInTheDocument();
        expect(screen.getByText("Distemper")).toBeInTheDocument();
    });

    it("shows loading state", async () => {
        // arrange
        let resolveLoad!: (v: any) => void;
        (apiClient.listPetVisits as any).mockReturnValue(
            new Promise(resolve => { resolveLoad = resolve; })
        );

        // act
        renderPage();

        // act + assert
        expect(screen.getByText("Loading...")).toBeInTheDocument();

        resolveLoad(VISITS);
        await screen.findByText("drsmith");
    });

    it("shows empty state", async () => {
        // arrange
        (apiClient.listPetVisits as any).mockResolvedValue([]);

        // act
        renderPage();

        // assert
        expect(await screen.findByText("No visits found.")).toBeInTheDocument();
    });

    it("shows error on fetch failure", async () => {
        // arrange
        (apiClient.listPetVisits as any).mockRejectedValue(new Error("Server error"));

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Server error")).toBeInTheDocument();
    });

    it("shows dash for null fields", async () => {
        // arrange
        const visitsWithNulls = [
            {id: 3, vetUsername: "drnull", startsAt: "2025-08-01T09:00:00", ownerSummary: null, vaccination: null},
        ];
        (apiClient.listPetVisits as any).mockResolvedValue(visitsWithNulls);

        // act
        renderPage();

        // assert
        await screen.findByText("drnull");
        const dashes = screen.getAllByText("—");
        expect(dashes.length).toBeGreaterThanOrEqual(2);
    });

    it("back button navigates to pets", async () => {
        // arrange
        (apiClient.listPetVisits as any).mockResolvedValue([]);

        renderPage();
        await screen.findByText("No visits found.");

        // act
        fireEvent.click(screen.getByRole("button", {name: /back/i}));

        // assert
        expect(mockNavigate).toHaveBeenCalledWith("/pets");
    });
});
