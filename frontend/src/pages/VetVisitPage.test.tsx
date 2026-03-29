import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import {vi} from "vitest";
import VetVisitPage from "./VetVisitPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        getVetVisit: vi.fn(),
        saveVetVisit: vi.fn(),
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

const VISIT = {id: 42, vetSummary: "All good", vaccination: "Rabies", ownerSummary: "Pet was calm"};

function renderPage(appointmentId = "5") {
    return render(
        <MemoryRouter initialEntries={[`/appointments/vet/visit/${appointmentId}`]}>
            <Routes>
                <Route path="/appointments/vet/visit/:appointmentId" element={<VetVisitPage/>}/>
            </Routes>
        </MemoryRouter>
    );
}

describe("VetVisitPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockNavigate.mockReset();
    });

    it("renders form fields", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);

        // act
        renderPage();

        // assert
        expect(await screen.findByLabelText("Vet Summary")).toBeInTheDocument();
        expect(screen.getByLabelText("Vaccination")).toBeInTheDocument();
        expect(screen.getByLabelText("Owner Summary")).toBeInTheDocument();
    });

    it("populates form with fetched data", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);

        // act
        renderPage();

        // assert
        expect(await screen.findByDisplayValue("All good")).toBeInTheDocument();
        expect(screen.getByDisplayValue("Rabies")).toBeInTheDocument();
        expect(screen.getByDisplayValue("Pet was calm")).toBeInTheDocument();
    });

    it("populates ownerSummary field with fetched data", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);

        // act
        renderPage();

        // assert
        expect(await screen.findByDisplayValue("Pet was calm")).toBeInTheDocument();
    });

    it("shows loading state", async () => {
        // arrange
        let resolveLoad!: (v: any) => void;
        (apiClient.getVetVisit as any).mockReturnValue(
            new Promise(resolve => { resolveLoad = resolve; })
        );

        // act
        renderPage();

        // act + assert
        expect(screen.getByText("Loading...")).toBeInTheDocument();

        resolveLoad(VISIT);
        await screen.findByLabelText("Vet Summary");
    });

    it("shows error on fetch failure", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockRejectedValue(new Error("Not found"));

        // act
        renderPage();

        // assert
        expect(await screen.findByText("Not found")).toBeInTheDocument();
    });

    it("save calls saveVetVisit with form values", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);
        (apiClient.saveVetVisit as any).mockResolvedValue(VISIT);

        renderPage();
        await screen.findByLabelText("Vet Summary");

        // act
        fireEvent.change(screen.getByLabelText("Vet Summary"), {target: {value: "Updated summary"}});
        fireEvent.change(screen.getByLabelText("Vaccination"), {target: {value: "Distemper"}});
        fireEvent.change(screen.getByLabelText("Owner Summary"), {target: {value: "Owner notes"}});
        fireEvent.click(screen.getByRole("button", {name: /save/i}));

        // assert
        await waitFor(() => {
            expect(apiClient.saveVetVisit).toHaveBeenCalledWith(5, {
                vetSummary: "Updated summary",
                vaccination: "Distemper",
                ownerSummary: "Owner notes",
            });
        });
    });

    it("shows success feedback after save", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);
        (apiClient.saveVetVisit as any).mockResolvedValue(VISIT);

        renderPage();
        await screen.findByLabelText("Vet Summary");

        // act
        fireEvent.click(screen.getByRole("button", {name: /save/i}));

        // assert
        expect(await screen.findByText("Saved successfully.")).toBeInTheDocument();
    });

    it("shows error feedback on save failure", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);
        (apiClient.saveVetVisit as any).mockRejectedValue(new Error("Save error"));

        renderPage();
        await screen.findByLabelText("Vet Summary");

        // act
        fireEvent.click(screen.getByRole("button", {name: /save/i}));

        // assert
        expect(await screen.findByText("Save error")).toBeInTheDocument();
    });

    it("back button navigates to /appointments/vet", async () => {
        // arrange
        (apiClient.getVetVisit as any).mockResolvedValue(VISIT);

        renderPage();
        await screen.findByLabelText("Vet Summary");

        // act
        fireEvent.click(screen.getByRole("button", {name: /back/i}));

        // assert
        expect(mockNavigate).toHaveBeenCalledWith("/appointments/vet");
    });
});
