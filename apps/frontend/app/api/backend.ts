import { varsleManglerTilgang } from "~/utils/manglerTilgangEvent";

export interface ApiResponse<T = unknown> {
  data?: T;
  status?: string;
  httpStatus?: number;
  melding?: string;
}

export async function apiCall<T = unknown>(
  endpoint: string,
  options?: RequestInit
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(`/api${endpoint}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options?.headers,
      },
    });

    if (response.status === 204) {
      return { data: undefined };
    }

    const data = await response.json();

    if (!response.ok) {
      if (response.status === 403) {
        varsleManglerTilgang();
      }
      return {
        status: data?.status,
        httpStatus: response.status,
        melding: data?.melding,
      };
    }

    return { data };
  } catch (error) {
    console.error("API call error:", error);
    return {
      melding: error instanceof Error ? error.message : "Ukjent feil",
    };
  }
}
