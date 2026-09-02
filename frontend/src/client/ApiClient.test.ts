import { describe, it, expect, vi, afterEach } from "vitest";
import ApiClient from "./ApiClient";
import type { PetRequest } from "./dto/PetRequest.ts";
import type { AppointmentRequest } from "./dto/AppointmentRequest.ts";
import type { Location } from "./dto/Location.ts";
import type { RegisterRequest } from "./dto/RegisterRequest.ts";
import type { VetVisitRequest } from "./dto/VetVisitRequest.ts";
import type { LoginRequest } from "./dto/LoginRequest.ts";

function mockFetch(body: unknown, status = 200) {
    const requests: Request[] = [];
    const fn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
        const req = input instanceof Request ? input : new Request(input, init);
        requests.push(req.clone());
        return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
    });
    return { fn, requests };
}

afterEach(() => {
    localStorage.clear();
});

describe("ApiClient canary", () => {
    it("listPets issues a GET to /api/pets and returns parsed JSON", async () => {
        // arrange
        const pets = [{ id: 1, name: "Fluffy" }];
        const { fn, requests } = mockFetch(pets);
        const client = new ApiClient("/api", fn);

        // act
        const result = await client.listPets();

        // assert
        expect(requests[0].method).toBe("GET");
        expect(new URL(requests[0].url).pathname).toBe("/api/pets");
        expect(result).toEqual(pets);
    });
});

type SimpleCase = {
    name: string;
    invoke: (client: ApiClient) => Promise<unknown>;
    method: string;
    path: string;
    expectsBody?: boolean;
};

const responseBody = { ok: true, marker: "response-payload" };

const simpleCases: SimpleCase[] = [
    {
        name: "loginUser",
        invoke: (c) => c.loginUser({ username: "user", password: "password123" } as LoginRequest),
        method: "POST",
        path: "/api/auth/login",
    },
    { name: "fetchAboutMe", invoke: (c) => c.fetchAboutMe(), method: "GET", path: "/api/users/aboutme" },
    { name: "listPets", invoke: (c) => c.listPets(), method: "GET", path: "/api/pets" },
    { name: "deletePet", invoke: (c) => c.deletePet(7), method: "DELETE", path: "/api/pets/7" },
    { name: "listAppointments", invoke: (c) => c.listAppointments(), method: "GET", path: "/api/owner/appointments" },
    {
        name: "createAppointment",
        invoke: (c) =>
            c.createAppointment({
                locationId: 3,
                petId: 4,
                startsAt: "2026-09-10T10:00:00",
                appointmentType: "CHECKUP",
            } as AppointmentRequest),
        method: "POST",
        path: "/api/owner/appointments",
    },
    { name: "cancelAppointment", invoke: (c) => c.cancelAppointment(9), method: "DELETE", path: "/api/owner/appointments/9" },
    {
        name: "rescheduleAppointment",
        invoke: (c) => c.rescheduleAppointment(11, "2026-09-11T09:00:00"),
        method: "PUT",
        path: "/api/owner/appointments/11/reschedule",
    },
    { name: "listVetAppointments", invoke: (c) => c.listVetAppointments(), method: "GET", path: "/api/vet/appointments" },
    { name: "cancelVetAppointment", invoke: (c) => c.cancelVetAppointment(12), method: "DELETE", path: "/api/vet/appointments/12" },
    { name: "confirmVetAppointment", invoke: (c) => c.confirmVetAppointment(13), method: "PUT", path: "/api/vet/appointments/13/confirm" },
    {
        name: "markVetAppointmentNoShow",
        invoke: (c) => c.markVetAppointmentNoShow(14),
        method: "PUT",
        path: "/api/vet/appointments/14/no-show",
    },
    { name: "listVets", invoke: (c) => c.listVets(), method: "GET", path: "/api/vets" },
    { name: "listLocations", invoke: (c) => c.listLocations(), method: "GET", path: "/api/locations" },
    { name: "listBookableLocations", invoke: (c) => c.listBookableLocations(), method: "GET", path: "/api/owner/locations" },
    { name: "retrieveLocations", invoke: (c) => c.retrieveLocations(21), method: "GET", path: "/api/locations/21" },
    { name: "deleteLocations", invoke: (c) => c.deleteLocations(22), method: "DELETE", path: "/api/locations/22" },
    { name: "getVetVisit", invoke: (c) => c.getVetVisit(31), method: "GET", path: "/api/vet/visits/31" },
    { name: "listPetVisits", invoke: (c) => c.listPetVisits(41), method: "GET", path: "/api/owner/pets/41/visits" },
    {
        name: "saveVetVisit",
        invoke: (c) =>
            c.saveVetVisit(51, { vetSummary: "summary", ownerSummary: "owner", vaccination: "rabies" } as VetVisitRequest),
        method: "PUT",
        path: "/api/vet/visits/51",
    },
    { name: "listAllUsers", invoke: (c) => c.listAllUsers(), method: "GET", path: "/api/admin/users" },
    { name: "deactivateUser", invoke: (c) => c.deactivateUser(61), method: "PUT", path: "/api/admin/users/61/deactivate" },
    { name: "activateUser", invoke: (c) => c.activateUser(62), method: "PUT", path: "/api/admin/users/62/activate" },
    { name: "listActivityLogs", invoke: (c) => c.listActivityLogs(), method: "GET", path: "/api/admin/activity-logs" },
    { name: "getStats", invoke: (c) => c.getStats(), method: "GET", path: "/api/admin/stats" },
];

describe.each(simpleCases)("ApiClient.$name", ({ invoke, method, path }) => {
    it(`sends ${method} ${path} and resolves with the response payload`, async () => {
        // arrange
        const { fn, requests } = mockFetch(responseBody);
        const client = new ApiClient("/api", fn);

        // act
        const result = await invoke(client);

        // assert
        expect(requests[0].method).toBe(method);
        expect(new URL(requests[0].url).pathname).toBe(path);
        if (result !== undefined) {
            expect(result).toEqual(responseBody);
        }
    });
});

describe("registerUser", () => {
    it("returns the raw Response object rather than parsed JSON", async () => {
        // arrange
        const { fn, requests } = mockFetch({ ignored: true }, 201);
        const client = new ApiClient("/api", fn);
        const payload: RegisterRequest = { username: "newuser", password: "password123", type: "OWNER" };

        // act
        const result = await client.registerUser(payload);

        // assert
        expect(requests[0].method).toBe("POST");
        expect(new URL(requests[0].url).pathname).toBe("/api/users/register");
        expect(result).toBeInstanceOf(Response);
        expect(result.status).toBe(201);
        expect(result.ok).toBe(true);
    });
});

describe("savePet", () => {
    it("POSTs to /api/pets when the payload has no id", async () => {
        // arrange
        const pet = { id: 5, name: "Rex" };
        const { fn, requests } = mockFetch(pet);
        const client = new ApiClient("/api", fn);
        const payload: PetRequest = { name: "Rex", species: "DOG" };

        // act
        const result = await client.savePet(payload);

        // assert
        expect(requests[0].method).toBe("POST");
        expect(new URL(requests[0].url).pathname).toBe("/api/pets");
        expect(await requests[0].json()).toEqual(payload);
        expect(result).toEqual(pet);
    });

    it("PUTs to /api/pets/{id} when the payload has an id", async () => {
        // arrange
        const pet = { id: 5, name: "Rex" };
        const { fn, requests } = mockFetch(pet);
        const client = new ApiClient("/api", fn);
        const payload: PetRequest & { id?: number } = { id: 5, name: "Rex", species: "DOG" };

        // act
        const result = await client.savePet(payload);

        // assert
        expect(requests[0].method).toBe("PUT");
        expect(new URL(requests[0].url).pathname).toBe("/api/pets/5");
        expect(result).toEqual(pet);
    });
});

describe("saveLocation", () => {
    it("POSTs to /api/locations when the payload has no id", async () => {
        // arrange
        const location = { id: 8, name: "Main Clinic" };
        const { fn, requests } = mockFetch(location);
        const client = new ApiClient("/api", fn);
        const payload: Location = { name: "Main Clinic", zoneId: "Europe/Vienna" };

        // act
        const result = await client.saveLocation(payload);

        // assert
        expect(requests[0].method).toBe("POST");
        expect(new URL(requests[0].url).pathname).toBe("/api/locations");
        expect(result).toEqual(location);
    });

    it("PUTs to /api/locations/{id} when the payload has an id", async () => {
        // arrange
        const location = { id: 8, name: "Main Clinic" };
        const { fn, requests } = mockFetch(location);
        const client = new ApiClient("/api", fn);
        const payload: Location = { id: 8, name: "Main Clinic", zoneId: "Europe/Vienna" };

        // act
        const result = await client.saveLocation(payload);

        // assert
        expect(requests[0].method).toBe("PUT");
        expect(new URL(requests[0].url).pathname).toBe("/api/locations/8");
        expect(result).toEqual(location);
    });
});

describe("listAvailableSlots", () => {
    it("substitutes locationId into the path and sends date/appointmentType as query params", async () => {
        // arrange
        const slots = [{ startsAt: "2026-09-10T09:00:00", endsAt: "2026-09-10T09:30:00" }];
        const { fn, requests } = mockFetch(slots);
        const client = new ApiClient("/api", fn);

        // act
        const result = await client.listAvailableSlots(17, "2026-09-10", "CHECKUP");

        // assert
        const url = new URL(requests[0].url);
        expect(requests[0].method).toBe("GET");
        expect(url.pathname).toBe("/api/owner/locations/17/available-slots");
        expect(url.searchParams.get("date")).toBe("2026-09-10");
        expect(url.searchParams.get("appointmentType")).toBe("CHECKUP");
        expect(result).toEqual(slots);
    });
});

describe("JWT header injection", () => {
    it("attaches the Authorization header when a jwt is present in localStorage", async () => {
        // arrange
        localStorage.setItem("jwt", "abc");
        const { fn, requests } = mockFetch([]);
        const client = new ApiClient("/api", fn);

        // act
        await client.listPets();

        // assert
        expect(requests[0].headers.get("Authorization")).toBe("Bearer abc");
    });

    it("omits the Authorization header when no jwt is stored", async () => {
        // arrange
        const { fn, requests } = mockFetch([]);
        const client = new ApiClient("/api", fn);

        // act
        await client.listPets();

        // assert
        expect(requests[0].headers.get("Authorization")).toBeNull();
    });
});

describe("error-unwrapping precedence", () => {
    it("uses the detail field when present", async () => {
        // arrange
        const { fn } = mockFetch({ detail: "Something specific went wrong" }, 422);
        const client = new ApiClient("/api", fn);

        // act + assert
        await expect(client.fetchAboutMe()).rejects.toThrow("Something specific went wrong");
    });

    it("falls back to the title field when detail is absent", async () => {
        // arrange
        const { fn } = mockFetch({ title: "Unprocessable Entity" }, 422);
        const client = new ApiClient("/api", fn);

        // act + assert
        await expect(client.fetchAboutMe()).rejects.toThrow("Unprocessable Entity");
    });

    it("falls back to a generic status message when neither detail nor title is present", async () => {
        // arrange
        const { fn } = mockFetch({}, 404);
        const client = new ApiClient("/api", fn);

        // act + assert
        await expect(client.fetchAboutMe()).rejects.toThrow("Request failed with status 404");
    });
});

describe("void-method error propagation", () => {
    it("throws when the mocked response is an error status", async () => {
        // arrange
        const { fn } = mockFetch({ detail: "Pet not found" }, 404);
        const client = new ApiClient("/api", fn);

        // act + assert
        await expect(client.deletePet(1)).rejects.toThrow("Pet not found");
    });
});
