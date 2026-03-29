import type {PetRequest} from "./dto/PetRequest.tsx";
import type {Pet} from "./dto/Pet.tsx";
import type {AppointmentRequest} from "./dto/AppointmentRequest.tsx";
import type {Appointment} from "./dto/Appointment.tsx";
import type {VetAppointment} from "./dto/VetAppointment.tsx";
import type {VetVisit} from "./dto/VetVisit.tsx";
import type {Vet} from "./dto/Vet.tsx";
import type {Location} from "./dto/Location.tsx";
import type {RegisterRequest} from "./dto/RegisterRequest.tsx";
import type {LoginResponse} from "./dto/LoginResponse.tsx";
import type {LoginRequest} from "./dto/LoginRequest.tsx";
import type {UserResponse} from "./dto/UserResponse.tsx";

export default class ApiClient {
    private baseUrl: string;

    constructor(baseUrl = "/api") {
        this.baseUrl = baseUrl.replace(/\/+$/, "");
    }

    private buildHeaders(extra?: Record<string, string>) {
        const jwt = localStorage.getItem("jwt");
        const headers: Record<string, string> = {
            Accept: "application/json",
            ...extra,
        };
        if (jwt) headers["Authorization"] = `Bearer ${jwt}`;
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

    async fetchAboutMe(): Promise<UserResponse> {
        const res = await fetch(`${this.baseUrl}/users/aboutme`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Failed to load pets: ${res.status}`);
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

    async listAppointments(): Promise<Appointment[]> {
        const res = await fetch(`${this.baseUrl}/owner/appointments`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Failed to load appointments: ${res.status}`);
        }
        return await res.json();
    }

    async createAppointment(payload: AppointmentRequest): Promise<Appointment> {
        const res = await fetch(`${this.baseUrl}/owner/appointments`, {
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

    async cancelAppointment(id: number): Promise<void> {
        const res = await fetch(`${this.baseUrl}/owner/appointments/${id}`, {
            method: "DELETE",
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Cancel failed: ${res.status}`);
        }
    }

    async listVetAppointments(): Promise<VetAppointment[]> {
        const res = await fetch(`${this.baseUrl}/vet/appointments`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Failed to load appointments: ${res.status}`);
        }
        return await res.json();
    }

    async cancelVetAppointment(id: number): Promise<void> {
        const res = await fetch(`${this.baseUrl}/vet/appointments/${id}`, {
            method: "DELETE",
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Cancel failed: ${res.status}`);
        }
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

    async getVetVisit(appointmentId: number): Promise<VetVisit> {
        const res = await fetch(`${this.baseUrl}/vet/visits/${appointmentId}`, {
            headers: this.buildHeaders(),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Failed to load visit: ${res.status}`);
        }
        return await res.json();
    }

    async saveVetVisit(appointmentId: number, payload: { vetSummary: string; vaccination: string }): Promise<VetVisit> {
        const res = await fetch(`${this.baseUrl}/vet/visits/${appointmentId}`, {
            method: "PUT",
            headers: this.buildHeaders({
                "Content-Type": "application/json",
            }),
            body: JSON.stringify(payload),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(text || `Save failed: ${res.status}`);
        }
        return await res.json();
    }

}

export const apiClient = new ApiClient();
