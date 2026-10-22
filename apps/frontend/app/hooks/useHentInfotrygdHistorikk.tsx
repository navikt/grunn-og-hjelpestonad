import { useEffect, useState } from "react";
import { hentStønaderForPerson } from "~/api/generated/sdk.gen";
import type { InfotrygdStonaderResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface HistorikkState {
  data: InfotrygdStonaderResponse | null;
  melding: string | null;
  laster: boolean;
}

export const useHentInfotrygdHistorikk = (personident: string | undefined) => {
  const [state, settState] = useState<HistorikkState>({
    data: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentHistorikk = async () => {
      if (!personident) {
        settState({ data: null, melding: null, laster: false });
        return;
      }

      try {
        const { data } = await hentStønaderForPerson({
          body: { personident, stønadstype: "GRUNNSTØNAD" },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;
        settState({ data, melding: null, laster: false });
      } catch (feil: unknown) {
        if (signal.aborted) return;
        settState({
          data: null,
          melding: feilmeldingFra(
            feil,
            "Vi kunne ikke hente vedtak fra Infotrygd. Prøv igjen senere."
          ),
          laster: false,
        });
      }
    };

    void hentHistorikk();

    return () => controller.abort();
  }, [personident]);

  return state;
};
