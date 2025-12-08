import type {PetRequest} from "./dto/PetRequest.tsx";
import type {Pet} from "./dto/Pet.tsx";
import type {AppointmentRequest} from "./dto/AppointmentRequest.tsx";
import type {Appointment} from "./dto/Appointment.tsx";
import type {Vet} from "./dto/Vet.tsx";
import type {Location} from "./dto/Location.tsx";
import type {RegisterRequest} from "./dto/RegisterRequest.tsx";
import type {LoginResponse} from "./dto/LoginResponse.tsx";
import type {LoginRequest} from "./dto/LoginRequest.tsx";

export default class ApiClient {
    private baseUrl: string;
    private getToken: () => string | null;

    constructor(getToken: () => string | null, baseUrl = "/api") {
        this.getToken = getToken;
        this.baseUrl = baseUrl.replace(/\/+$/, "");
    }

    private buildHeaders(extra?: Record<string, string>) {
        const token = this.getToken();
        const headers: Record<string, string> = {
            Accept: "application/json",
            ...extra,
        };
        if (token) headers["Authorization"] = `Bearer ${token}`;
        return headers;
    }

    async registerUser(payload: RegisterRequest) {
        return await fetch(`${this.baseUrl}/users/register`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload),
        });
    }

    async loginUser(payload: LoginRequest): Promise<LoginResponse> {
        const res = await fetch(`${this.baseUrl}/auth/login`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Login failed`);
        }

        return await res.json();

    }

    async listPets(): Promise<Pet[]> {
        const res = await fetch(`${this.baseUrl}/pets`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Failed to load pets: ${res.status}`);
        }
        return await res.json();
    }

    async createPet(payload: PetRequest): Promise<Pet> {
        const res = await fetch(`${this.baseUrl}/pets`, {
            method: "POST",
            headers: this.buildHeaders({
                "Content-Type": "application/json",
            }),
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Server returned ${res.status}`);
        }

        return await res.json();
    }

    async createAppointment(payload: AppointmentRequest): Promise<Appointment> {
        const res = await fetch(`${this.baseUrl}/appointments`, {
            method: "POST",
            headers: this.buildHeaders({
                "Content-Type": "application/json",
            }),
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Server returned ${res.status}`);
        }

        return await res.json();
    }

    async listVets(): Promise<Vet[]> {
        const res = await fetch(`${this.baseUrl}/vets`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Failed to load pets: ${res.status}`);
        }
        return await res.json();
    }

    async saveLocation(payload: Location): Promise<Location> {
        const method = payload.id ? "PUT" : "POST";
        const url = payload.id ? `${this.baseUrl}/locations/${payload.id}` : `${this.baseUrl}//locations`;
        const res = await fetch(url, {
            method,
            headers: this.buildHeaders({
                "Content-Type": "application/json",
            }),
            body: JSON.stringify(payload),
        });
        if (!(res.ok || res.status === 201)) {
            const txt = await res.text();
            throw new Error(txt || `Save failed: ${res.status}`);
        }
        return await res.json();
    }

    async listLocations(): Promise<Location[]> {
        const res = await fetch(`${this.baseUrl}/locations`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) throw new Error(`Failed to fetch locations: ${res.status}`);
        return await res.json();
    }

    async retrieveLocations(id: number): Promise<Location> {
        const res = await fetch(`${this.baseUrl}/locations/${id}`, {
            headers: this.buildHeaders()
        });
        if (!res.ok) throw new Error(`Failed to load location ${id}: ${res.status}`);
        return await res.json();
    }

    async deleteLocations(id: number): Promise<void> {
        const res = await fetch(`${this.baseUrl}/locations/${id}`, {
            method: "DELETE",
            headers: this.buildHeaders()
        });
        if (!res.ok) throw new Error(`Delete failed: ${res.status}`);
    }

}
