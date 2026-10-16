import { useEffect, useState } from "react";
import { hentBehandlinger as hentBehandlingerApi } from "~/api/generated/sdk.gen";
import type { BehandlingResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface BehandlingerState {
  behandlinger: BehandlingResponse[] | null;
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
    const controller = new AbortController();
    const { signal } = controller;

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

      try {
        const { data } = await hentBehandlingerApi({
          body: { fagsakId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;
        settState((prev) => ({
          ...prev,
          behandlinger: data,
          melding: null,
          laster: false,
        }));
      } catch (feil: unknown) {
        if (signal.aborted) return;
        settState((prev) => ({
          ...prev,
          melding: feilmeldingFra(feil, "Vi kunne ikke hente behandlingene. Prøv igjen senere."),
          laster: false,
        }));
      }
    };

    void hentBehandlinger();
    return () => controller.abort();
  }, [fagsakId]);

  return state;
}
