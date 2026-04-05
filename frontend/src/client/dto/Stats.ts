export interface VetAppointmentCount {
  vetUsername: string;
  count: number;
}

export interface Stats {
  totalOwners: number;
  totalVets: number;
  totalPets: number;
  totalAppointments: number;
  appointmentsPerVet: VetAppointmentCount[];
}
