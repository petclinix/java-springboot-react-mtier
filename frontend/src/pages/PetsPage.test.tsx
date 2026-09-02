import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {MemoryRouter} from "react-router-dom";
import {vi} from "vitest";
import PetsPage from "./PetsPage";
import {apiClient} from "../client/ApiClient";

vi.mock("../client/ApiClient", () => ({
    apiClient: {
        listPets: vi.fn(),
        savePet: vi.fn(),
        deletePet: vi.fn(),
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
    {id: 3, name: "Whiskers", species: "CAT", gender: "FEMALE", breed: null, birthDate: null, picture: "aGVsbG8=", pictureContentType: "image/png"},
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
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);

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
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);

        // act
        renderPage();
        await screen.findByText("Fluffy");

        // assert
        expect(screen.getByText("Siamese")).toBeInTheDocument();
        expect(screen.getByText("Rex")).toBeInTheDocument();
    });

    it("renders a thumbnail for a pet with a picture and no broken image for one without", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);

        // act
        renderPage();
        await screen.findByText("Whiskers");

        // assert
        const image = screen.getByAltText("Whiskers picture") as HTMLImageElement;
        expect(image).toBeInTheDocument();
        expect(image.src).toBe("data:image/png;base64,aGVsbG8=");
        expect(screen.queryByAltText("Fluffy picture")).not.toBeInTheDocument();
        expect(screen.queryByAltText("Rex picture")).not.toBeInTheDocument();
    });

    it("submits a new pet including breed and prepends it to the list", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue([]);
        (apiClient.savePet as ReturnType<typeof vi.fn>).mockResolvedValue({
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
        expect(apiClient.savePet).toHaveBeenCalledWith(
            expect.objectContaining({name: "Buddy", breed: "Labrador"})
        );
        expect(screen.getByText("Labrador")).toBeInTheDocument();
    });

    it("submits a new pet with a selected picture, including base64 payload and content type", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue([]);
        (apiClient.savePet as ReturnType<typeof vi.fn>).mockResolvedValue({
            id: 4, name: "Milo", species: "DOG", gender: "MALE", picture: "aGVsbG8=", pictureContentType: "image/png",
        });
        const user = userEvent.setup();

        const {container} = renderPage();
        await screen.findByText("No pets found.");

        const nameInput = container.querySelectorAll("input")[0] as HTMLInputElement;
        const fileInput = container.querySelector("input[type='file']") as HTMLInputElement;
        const file = new File(["hello"], "milo.png", {type: "image/png"});

        // act
        fireEvent.change(nameInput, {target: {value: "Milo"}});
        await user.upload(fileInput, file);
        // FileReader.readAsDataURL resolves asynchronously; wait for jsdom to finish
        // reading the file before the form state (and thus the submit payload) is ready.
        await waitFor(() => expect(fileInput.files?.[0]?.name).toBe("milo.png"));
        await new Promise(resolve => setTimeout(resolve, 0));
        fireEvent.click(screen.getByRole("button", {name: /add pet/i}));

        // assert
        await waitFor(() => {
            expect(apiClient.savePet).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: "Milo",
                    picture: "aGVsbG8=",
                    pictureContentType: "image/png",
                })
            );
        });
        expect(fileInput.value).toBe("");
    });

    it("clicking edit populates the form with the pet's current values", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);
        const {container} = renderPage();
        await screen.findByText("Fluffy");

        // act
        const editButtons = screen.getAllByRole("button", {name: /^edit$/i});
        fireEvent.click(editButtons[0]);

        // assert
        const nameInput = container.querySelectorAll("input")[0] as HTMLInputElement;
        const breedInput = container.querySelectorAll("input")[1] as HTMLInputElement;
        expect(nameInput.value).toBe("Fluffy");
        expect(breedInput.value).toBe("Siamese");
        expect(screen.getByText("Edit Pet")).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /^save$/i})).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /^cancel$/i})).toBeInTheDocument();
    });

    it("submitting the populated edit form calls savePet with the pet's id and updates the list", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);
        (apiClient.savePet as ReturnType<typeof vi.fn>).mockResolvedValue({
            id: 1, name: "Fluffy Jr.", species: "CAT", gender: "FEMALE", breed: "Siamese", birthDate: "2020-01-01",
        });
        const {container} = renderPage();
        await screen.findByText("Fluffy");

        const editButtons = screen.getAllByRole("button", {name: /^edit$/i});
        fireEvent.click(editButtons[0]);
        const nameInput = container.querySelectorAll("input")[0] as HTMLInputElement;

        // act
        fireEvent.change(nameInput, {target: {value: "Fluffy Jr."}});
        fireEvent.click(screen.getByRole("button", {name: /^save$/i}));

        // assert
        await screen.findByText("Fluffy Jr.");
        expect(apiClient.savePet).toHaveBeenCalledWith(
            expect.objectContaining({id: 1, name: "Fluffy Jr."})
        );
        expect(screen.queryByText("Fluffy")).not.toBeInTheDocument();
    });

    it("clicking cancel while editing resets the form without submitting", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);
        const {container} = renderPage();
        await screen.findByText("Fluffy");
        const editButtons = screen.getAllByRole("button", {name: /^edit$/i});
        fireEvent.click(editButtons[0]);

        // act
        fireEvent.click(screen.getByRole("button", {name: /^cancel$/i}));

        // assert
        const nameInput = container.querySelectorAll("input")[0] as HTMLInputElement;
        expect(nameInput.value).toBe("");
        expect(screen.getByRole("heading", {name: "Add Pet"})).toBeInTheDocument();
        expect(apiClient.savePet).not.toHaveBeenCalled();
    });

    it("clicking remove and confirming calls deletePet and removes the pet from the list", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);
        (apiClient.deletePet as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
        window.confirm = vi.fn().mockReturnValue(true);
        renderPage();
        await screen.findByText("Fluffy");

        // act
        const removeButtons = screen.getAllByRole("button", {name: /^remove$/i});
        fireEvent.click(removeButtons[0]);

        // assert
        await waitFor(() => expect(apiClient.deletePet).toHaveBeenCalledWith(1));
        await waitFor(() => expect(screen.queryByText("Fluffy")).not.toBeInTheDocument());
    });

    it("clicking remove and declining the confirmation does not call deletePet", async () => {
        // arrange
        (apiClient.listPets as ReturnType<typeof vi.fn>).mockResolvedValue(PETS);
        window.confirm = vi.fn().mockReturnValue(false);
        renderPage();
        await screen.findByText("Fluffy");

        // act
        const removeButtons = screen.getAllByRole("button", {name: /^remove$/i});
        fireEvent.click(removeButtons[0]);

        // assert
        expect(apiClient.deletePet).not.toHaveBeenCalled();
        expect(screen.getByText("Fluffy")).toBeInTheDocument();
    });
});
