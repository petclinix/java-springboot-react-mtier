export type Location = {
    id?: number;
    name: string;
    zoneId: string;
    weeklyPeriods: OpeningPeriod[];
    exceptions: OpeningException[];
};

export type OpeningPeriod = {
    dayOfWeek: number;
    startTime: string;
    endTime: string;
    sortOrder: number;
};

export type OpeningException = {
    date: string;
    closed: boolean;
    note?: string | null;
    periods: OpeningExceptionPeriod[];
};

export type OpeningExceptionPeriod = {
    startTime: string;
    endTime: string;
    sortOrder: number;
};

