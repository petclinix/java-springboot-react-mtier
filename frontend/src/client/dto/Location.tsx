export type Location = {
    id?: number;
    name: string;
    zoneId: string;
    weeklyPeriods: OpeningPeriod[];
    overrides: OpeningOverride[];
};

export type OpeningPeriod = {
    dayOfWeek: number;
    startTime: string;
    endTime: string;
    sortOrder: number;
};

export type OpeningOverride = {
    date: string;
    openTime?: string | null;
    closeTime?: string | null;
    closed: boolean;
    reason?: string | null;
};
