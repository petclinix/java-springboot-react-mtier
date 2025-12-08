import type {CreatePet} from "./dto/CreatePet.tsx";
import type {Pet} from "./dto/Pet.tsx";
import type {AppointmentRequest} from "./dto/AppointmentRequest.tsx";
import type {Appointment} from "./dto/Appointment.tsx";
import type {Vet} from "./dto/Vet.tsx";

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

    async createPet(payload: CreatePet): Promise<Pet> {
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
        const res = await fetch("/api/appointments", {
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


}
