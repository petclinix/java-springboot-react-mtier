export interface paths {
    "/vet/visits/{appointmentId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get"];
        put: operations["put"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/vet/appointments/{id}/no-show": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["markNoShow"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/vet/appointments/{id}/confirm": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["confirm"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/pets/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["update"];
        post?: never;
        delete: operations["delete"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/owner/appointments/{id}/reschedule": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["reschedule"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/locations/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["retrieve"];
        put: operations["update_1"];
        post?: never;
        delete: operations["delete_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/users/{id}/deactivate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["deactivate"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/users/{id}/activate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["activate"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/users/register": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["register"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/pets": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["retrieveAll"];
        put?: never;
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/owner/appointments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list"];
        put?: never;
        post: operations["create_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/locations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["retrieveAll_1"];
        put?: never;
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/vets": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["retrieveAll_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/vet/appointments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/users/aboutme": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["aboutme"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/owner/pets/{petId}/visits": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/owner/locations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["retrieveAll_3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/owner/locations/{id}/available-slots": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["retrieveAvailableSlots"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getAll"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/stats": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/activity-logs": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/vet/appointments/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["cancel"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/owner/appointments/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["cancel_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        VetVisitRequest: {
            vetSummary?: string;
            ownerSummary?: string;
            vaccination?: string;
        };
        VetVisit: {
            /** Format: int64 */
            id?: number;
            vetSummary?: string;
            ownerSummary?: string;
            vaccination?: string;
        };
        PetRequest: {
            name: string;
            /** @enum {string} */
            species?: "DOG" | "CAT" | "BIRD" | "RABBIT" | "REPTILE" | "OTHER";
            breed?: string;
            /** @enum {string} */
            gender?: "MALE" | "FEMALE" | "UNKNOWN";
            /** Format: date */
            birthDate?: string;
            /** Format: byte */
            picture?: string;
            pictureContentType?: string;
        };
        Pet: {
            /** Format: int64 */
            id?: number;
            name?: string;
            species?: string;
            breed?: string;
            gender?: string;
            /** Format: date */
            birthDate?: string;
            /** Format: byte */
            picture?: string;
            pictureContentType?: string;
            active?: boolean;
        };
        RescheduleRequest: {
            /** Format: date-time */
            startsAt: string;
        };
        Appointment: {
            /** Format: int64 */
            id?: number;
            /** Format: int64 */
            vetId?: number;
            /** Format: int64 */
            petId?: number;
            /** Format: date-time */
            startsAt?: string;
            /** Format: int64 */
            locationId?: number;
            /** Format: date-time */
            endsAt?: string;
            /** @enum {string} */
            status?: "BOOKED" | "CONFIRMED" | "CANCELLED" | "COMPLETED" | "NO_SHOW";
            /** @enum {string} */
            appointmentType?: "VACCINATION" | "FOLLOW_UP" | "CHECKUP" | "EMERGENCY" | "SURGERY";
        };
        Location: {
            /** Format: int64 */
            id?: number;
            name?: string;
            zoneId?: string;
            street?: string;
            postalCode?: string;
            city?: string;
            country?: string;
            weeklyPeriods?: components["schemas"]["OpeningPeriodResponse"][];
            overrides?: components["schemas"]["OpeningOverrideResponse"][];
        };
        OpeningOverrideResponse: {
            /** Format: date */
            date?: string;
            openTime?: string;
            closeTime?: string;
            closed?: boolean;
            reason?: string;
        };
        OpeningPeriodResponse: {
            /** Format: int32 */
            dayOfWeek?: number;
            startTime?: string;
            endTime?: string;
            /** Format: int32 */
            sortOrder?: number;
        };
        AdminUserResponse: {
            /** Format: int64 */
            id?: number;
            username?: string;
            role?: string;
            active?: boolean;
            /** Format: date-time */
            lastLogin?: string;
        };
        RegisterRequest: {
            username: string;
            password: string;
            /** @enum {string} */
            type: "ADMIN" | "VET" | "OWNER";
        };
        UserResponse: {
            /** Format: int64 */
            id?: number;
            username?: string;
            role?: string;
        };
        AppointmentRequest: {
            /** Format: int64 */
            locationId: number;
            /** Format: int64 */
            petId: number;
            /** Format: date-time */
            startsAt: string;
            /** @enum {string} */
            appointmentType: "VACCINATION" | "FOLLOW_UP" | "CHECKUP" | "EMERGENCY" | "SURGERY";
        };
        LoginRequest: {
            username: string;
            password: string;
        };
        LoginResponse: {
            token?: string;
            type?: string;
        };
        Vet: {
            /** Format: int64 */
            id?: number;
            username?: string;
        };
        VetAppointment: {
            /** Format: int64 */
            id?: number;
            /** Format: int64 */
            petId?: number;
            petName?: string;
            ownerUsername?: string;
            /** Format: date-time */
            startsAt?: string;
            /** @enum {string} */
            status?: "BOOKED" | "CONFIRMED" | "CANCELLED" | "COMPLETED" | "NO_SHOW";
            /** @enum {string} */
            appointmentType?: "VACCINATION" | "FOLLOW_UP" | "CHECKUP" | "EMERGENCY" | "SURGERY";
        };
        OwnerVisit: {
            /** Format: int64 */
            id?: number;
            ownerSummary?: string;
            vaccination?: string;
            vetUsername?: string;
            /** Format: date-time */
            startsAt?: string;
        };
        BookableLocation: {
            /** Format: int64 */
            id?: number;
            name?: string;
            vetUsername?: string;
            zoneId?: string;
            street?: string;
            postalCode?: string;
            city?: string;
            country?: string;
        };
        AvailableSlot: {
            /** Format: date-time */
            startsAt?: string;
            /** Format: date-time */
            endsAt?: string;
        };
        StatsData: {
            /** Format: int64 */
            totalOwners?: number;
            /** Format: int64 */
            totalVets?: number;
            /** Format: int64 */
            totalPets?: number;
            /** Format: int64 */
            totalAppointments?: number;
            appointmentsPerVet?: components["schemas"]["VetAppointmentCount"][];
        };
        VetAppointmentCount: {
            vetUsername?: string;
            /** Format: int64 */
            count?: number;
        };
        ActivityLogEntry: {
            /** Format: int64 */
            id?: number;
            username?: string;
            action?: string;
            /** Format: date-time */
            timestamp?: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    get: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                appointmentId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["VetVisit"];
                };
            };
        };
    };
    put: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                appointmentId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["VetVisitRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["VetVisit"];
                };
            };
        };
    };
    markNoShow: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    confirm: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    update: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PetRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Pet"];
                };
            };
        };
    };
    delete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    reschedule: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RescheduleRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Appointment"];
                };
            };
        };
    };
    retrieve: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Location"];
                };
            };
        };
    };
    update_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["Location"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Location"];
                };
            };
        };
    };
    delete_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    deactivate: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AdminUserResponse"];
                };
            };
        };
    };
    activate: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AdminUserResponse"];
                };
            };
        };
    };
    register: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RegisterRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserResponse"];
                };
            };
        };
    };
    retrieveAll: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Pet"][];
                };
            };
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PetRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Pet"];
                };
            };
        };
    };
    list: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Appointment"][];
                };
            };
        };
    };
    create_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AppointmentRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Appointment"];
                };
            };
        };
    };
    retrieveAll_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Location"][];
                };
            };
        };
    };
    create_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["Location"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Location"];
                };
            };
        };
    };
    login: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LoginRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["LoginResponse"];
                };
            };
        };
    };
    retrieveAll_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Vet"][];
                };
            };
        };
    };
    list_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["VetAppointment"][];
                };
            };
        };
    };
    aboutme: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserResponse"];
                };
            };
        };
    };
    list_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                petId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OwnerVisit"][];
                };
            };
        };
    };
    retrieveAll_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["BookableLocation"][];
                };
            };
        };
    };
    retrieveAvailableSlots: {
        parameters: {
            query: {
                date: string;
                appointmentType: "VACCINATION" | "FOLLOW_UP" | "CHECKUP" | "EMERGENCY" | "SURGERY";
            };
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AvailableSlot"][];
                };
            };
        };
    };
    getAll: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AdminUserResponse"][];
                };
            };
        };
    };
    get_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["StatsData"];
                };
            };
        };
    };
    get_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ActivityLogEntry"][];
                };
            };
        };
    };
    cancel: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    cancel_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
}
