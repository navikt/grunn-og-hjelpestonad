import { useEffect, useState } from "react";
import { hentGjeldendePerioderForPerson } from "~/api/generated/sdk.gen";
import type { InfotrygdGjeldendePerioderResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface GjeldendePerioderState {
  data: InfotrygdGjeldendePerioderResponse | null;
  melding: string | null;
  laster: boolean;
}

export const useHentGjeldendeInfotrygdPerioder = (personident: string | undefined) => {
  const [state, settState] = useState<GjeldendePerioderState>({
    data: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentPerioder = async () => {
      if (!personident) {
        settState({ data: null, melding: null, laster: false });
        return;
      }

      try {
        const { data } = await hentGjeldendePerioderForPerson({
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
            "Vi kunne ikke hente gjeldende perioder fra Infotrygd. Prøv igjen senere."
          ),
          laster: false,
        });
      }
    };

    void hentPerioder();

    return () => controller.abort();
  }, [personident]);

  return state;
};
