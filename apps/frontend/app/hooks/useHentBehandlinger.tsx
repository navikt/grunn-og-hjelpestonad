import { useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import type { Behandling } from "~/types/behandling";

interface BehandlingerState {
  behandlinger: [Behandling] | null;
  melding: string | null;
  laster: boolean;
}

export function useHentBehandlinger(fagsakId: string | undefined) {
  const [state, settState] = useState<BehandlingerState>({
    behandlinger: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const hentBehandlingerForFagsak = async (
      fagsakId: string
    ): Promise<ApiResponse<[Behandling]>> => {
      return apiCall(`/behandling/hentBehandlinger`, {
        method: "POST",
        body: JSON.stringify({ fagsakId }),
      });
    };

    const hentBehandlinger = async () => {
      if (!fagsakId) {
        settState((prev) => ({
          ...prev,
          melding: "Mangler fagsakId",
          laster: false,
        }));
        return;
      }

      settState((prev) => ({ ...prev, melding: null, laster: true }));

      const response = await hentBehandlingerForFagsak(fagsakId);

      if (response.data) {
        settState((prev) => ({
          ...prev,
          behandlinger: response.data ?? null,
          laster: false,
        }));
      }
    };

    hentBehandlinger();
  }, [fagsakId]);

  return state;
}
