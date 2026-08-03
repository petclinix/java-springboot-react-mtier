export type AppointmentRequest = {
    locationId:  number,
    petId:  number,
    // Convert from the datetime-local value (which is local) to an ISO string
    startsAt: string,
};
