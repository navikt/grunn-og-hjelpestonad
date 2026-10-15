import { useEffect, useState } from "react";
import { hentDiagnoserForPerson } from "~/api/generated/sdk.gen";
import type { InfotrygdDiagnoserResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface DiagnoserState {
  data: InfotrygdDiagnoserResponse | null;
  melding: string | null;
  laster: boolean;
}

export const useHentInfotrygdDiagnoser = (personident: string | undefined) => {
  const [state, settState] = useState<DiagnoserState>({
    data: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentDiagnoser = async () => {
      if (!personident) {
        settState({ data: null, melding: null, laster: false });
        return;
      }

      try {
        const { data } = await hentDiagnoserForPerson({
          body: { personident, stønadstyper: [] },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;
        settState({ data, melding: null, laster: false });
      } catch (feil: unknown) {
        if (signal.aborted) return;
        settState({
          data: null,
          melding: feilmeldingFra(feil, "Vi kunne ikke hente diagnoser fra Infotrygd. Prøv igjen senere."),
          laster: false,
        });
      }
    };

    void hentDiagnoser();

    return () => controller.abort();
  }, [personident]);

  return state;
};
