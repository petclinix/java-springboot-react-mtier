import createClient from "openapi-fetch";
import type { paths } from "./generated/schema";
import type { PetRequest } from "./dto/PetRequest.tsx";
import type { Pet } from "./dto/Pet.tsx";
import type { AppointmentRequest } from "./dto/AppointmentRequest.tsx";
import type { Appointment } from "./dto/Appointment.tsx";
import type { RescheduleRequest } from "./dto/RescheduleRequest.ts";
import type { VetAppointment } from "./dto/VetAppointment.tsx";
import type { VetVisit } from "./dto/VetVisit.tsx";
import type { OwnerVisit } from "./dto/OwnerVisit.tsx";
import type { Vet } from "./dto/Vet.tsx";
import type { Location } from "./dto/Location.tsx";
import type { BookableLocation } from "./dto/BookableLocation.ts";
import type { AvailableSlot } from "./dto/AvailableSlot.ts";
import type { RegisterRequest } from "./dto/RegisterRequest.tsx";
import type { LoginResponse } from "./dto/LoginResponse.tsx";
import type { LoginRequest } from "./dto/LoginRequest.tsx";
import type { UserResponse } from "./dto/UserResponse.tsx";
import type { AdminUser } from "./dto/AdminUser.tsx";
import type { Stats } from "./dto/Stats.tsx";
import type { VetVisitRequest } from "./dto/VetVisitRequest.ts";
import type { ActivityLogEntry } from "./dto/ActivityLogEntry.ts";

export default class ApiClient {
    private readonly client: ReturnType<typeof createClient<paths>>;

    constructor(baseUrl = "/api") {
        this.client = createClient<paths>({ baseUrl: baseUrl.replace(/\/+$/, "") });
        this.client.use({
            onRequest({ request }) {
                const jwt = localStorage.getItem("jwt");
                if (jwt) request.headers.set("Authorization", `Bearer ${jwt}`);
                return request;
            },
        });
    }

    private async unwrap<T>(result: { data?: T; error?: unknown; response: Response }): Promise<T> {
        await this.assertNoError(result);
        return result.data as T;
    }

    private async describeError(error: unknown, response: Response): Promise<string> {
        if (error && typeof error === "object") {
            const problem = error as { detail?: string; title?: string };
            if (problem.detail) return problem.detail;
            if (problem.title) return problem.title;
        }
        return `Request failed with status ${response.status}`;
    }

    // Widens `error`/`response` to a stable shape so callers of void-returning
    // (delete/cancel) endpoints don't fall foul of openapi-fetch narrowing
    // `error` to exactly `undefined` when no error schema is modeled.
    private async assertNoError(result: { error?: unknown; response: Response }): Promise<void> {
        if (result.error !== undefined) {
            throw new Error(await this.describeError(result.error, result.response));
        }
    }

    async registerUser(payload: RegisterRequest) {
        const result = await this.client.POST("/users/register", { body: payload });
        return result.response;
    }

    async loginUser(payload: LoginRequest): Promise<LoginResponse> {
        const result = await this.client.POST("/auth/login", { body: payload });
        return this.unwrap(result);
    }

    async fetchAboutMe(): Promise<UserResponse> {
        const result = await this.client.GET("/users/aboutme");
        return this.unwrap(result);
    }

    async listPets(): Promise<Pet[]> {
        const result = await this.client.GET("/pets");
        return this.unwrap(result);
    }

    async savePet(payload: PetRequest & { id?: number }): Promise<Pet> {
        const result = payload.id
            ? await this.client.PUT("/pets/{id}", { params: { path: { id: payload.id } }, body: payload })
            : await this.client.POST("/pets", { body: payload });
        return this.unwrap(result);
    }

    async deletePet(id: number): Promise<void> {
        const result = await this.client.DELETE("/pets/{id}", { params: { path: { id } } });
        await this.assertNoError(result);
    }

    async listAppointments(): Promise<Appointment[]> {
        const result = await this.client.GET("/owner/appointments");
        return this.unwrap(result);
    }

    async createAppointment(payload: AppointmentRequest): Promise<Appointment> {
        const result = await this.client.POST("/owner/appointments", { body: payload });
        return this.unwrap(result);
    }

    async cancelAppointment(id: number): Promise<void> {
        const result = await this.client.DELETE("/owner/appointments/{id}", { params: { path: { id } } });
        await this.assertNoError(result);
    }

    async rescheduleAppointment(id: number, startsAt: string): Promise<Appointment> {
        const payload: RescheduleRequest = { startsAt };
        const result = await this.client.PUT("/owner/appointments/{id}/reschedule", {
            params: { path: { id } },
            body: payload,
        });
        return this.unwrap(result);
    }

    async listVetAppointments(): Promise<VetAppointment[]> {
        const result = await this.client.GET("/vet/appointments");
        return this.unwrap(result);
    }

    async cancelVetAppointment(id: number): Promise<void> {
        const result = await this.client.DELETE("/vet/appointments/{id}", { params: { path: { id } } });
        await this.assertNoError(result);
    }

    async confirmVetAppointment(id: number): Promise<void> {
        const result = await this.client.PUT("/vet/appointments/{id}/confirm", { params: { path: { id } } });
        await this.assertNoError(result);
    }

    async markVetAppointmentNoShow(id: number): Promise<void> {
        const result = await this.client.PUT("/vet/appointments/{id}/no-show", { params: { path: { id } } });
        await this.assertNoError(result);
    }

    async listVets(): Promise<Vet[]> {
        const result = await this.client.GET("/vets");
        return this.unwrap(result);
    }

    async saveLocation(payload: Location): Promise<Location> {
        const result = payload.id
            ? await this.client.PUT("/locations/{id}", { params: { path: { id: payload.id } }, body: payload })
            : await this.client.POST("/locations", { body: payload });
        return this.unwrap(result);
    }

    async listLocations(): Promise<Location[]> {
        const result = await this.client.GET("/locations");
        return this.unwrap(result);
    }

    async listBookableLocations(): Promise<BookableLocation[]> {
        const result = await this.client.GET("/owner/locations");
        return this.unwrap(result);
    }

    async listAvailableSlots(locationId: number, date: string, appointmentType: string): Promise<AvailableSlot[]> {
        const result = await this.client.GET("/owner/locations/{id}/available-slots", {
            params: {
                path: { id: locationId },
                // appointmentType is a plain string at this API boundary (see AppointmentRequest.appointmentType
                // handling elsewhere); narrowed here to the generated literal union expected by the query params.
                query: { date, appointmentType: appointmentType as AppointmentRequest["appointmentType"] },
            },
        });
        return this.unwrap(result);
    }

    async retrieveLocations(id: number): Promise<Location> {
        const result = await this.client.GET("/locations/{id}", { params: { path: { id } } });
        return this.unwrap(result);
    }

    async deleteLocations(id: number): Promise<void> {
        const result = await this.client.DELETE("/locations/{id}", { params: { path: { id } } });
        await this.assertNoError(result);
    }

    async getVetVisit(appointmentId: number): Promise<VetVisit> {
        const result = await this.client.GET("/vet/visits/{appointmentId}", { params: { path: { appointmentId } } });
        return this.unwrap(result);
    }

    async listPetVisits(petId: number): Promise<OwnerVisit[]> {
        const result = await this.client.GET("/owner/pets/{petId}/visits", { params: { path: { petId } } });
        return this.unwrap(result);
    }

    async saveVetVisit(appointmentId: number, payload: VetVisitRequest): Promise<VetVisit> {
        const result = await this.client.PUT("/vet/visits/{appointmentId}", {
            params: { path: { appointmentId } },
            body: payload,
        });
        return this.unwrap(result);
    }

    async listAllUsers(): Promise<AdminUser[]> {
        const result = await this.client.GET("/admin/users");
        return this.unwrap(result);
    }

    async deactivateUser(id: number): Promise<AdminUser> {
        const result = await this.client.PUT("/admin/users/{id}/deactivate", { params: { path: { id } } });
        return this.unwrap(result);
    }

    async activateUser(id: number): Promise<AdminUser> {
        const result = await this.client.PUT("/admin/users/{id}/activate", { params: { path: { id } } });
        return this.unwrap(result);
    }

    async listActivityLogs(): Promise<ActivityLogEntry[]> {
        const result = await this.client.GET("/admin/activity-logs");
        return this.unwrap(result);
    }

    async getStats(): Promise<Stats> {
        const result = await this.client.GET("/admin/stats");
        return this.unwrap(result);
    }
}

export const apiClient = new ApiClient();
