import {useMemo} from "react";
import ApiClient, {apiClient} from "../client/ApiClient.tsx";

export function useApiClient(): ApiClient {
    // memoize so components get stable reference
    return useMemo(() => apiClient, []);
}
