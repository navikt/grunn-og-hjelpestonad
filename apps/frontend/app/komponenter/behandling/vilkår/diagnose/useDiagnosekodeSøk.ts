import { useEffect, useState } from "react";
import { søkDiagnosekoder } from "~/api/generated/sdk.gen";
import type { DiagnosekodeResponse } from "~/types/vilkår";

const VENTETID_MS = 250;

interface SøkState {
  søketekst: string;
  treff: DiagnosekodeResponse[];
  feil?: unknown;
}

/**
 * Søker i ICD-10 i backend. Søket går som POST, så søketeksten – som kan være en
 * diagnose – ikke havner i URL-er som spores eller logges.
 */
export function useDiagnosekodeSøk(søketekst: string) {
  const [state, settState] = useState<SøkState | undefined>(undefined);
  const søk = søketekst.trim();

  useEffect(() => {
    if (søk === "") return;

    const controller = new AbortController();
    const { signal } = controller;
    const timer = setTimeout(async () => {
      try {
        const { data: treff } = await søkDiagnosekoder({
          body: { søketekst: søk },
          signal,
          throwOnError: true,
        });
        if (!signal.aborted) settState({ søketekst: søk, treff });
      } catch (feil) {
        if (!signal.aborted) settState({ søketekst: søk, treff: [], feil });
      }
    }, VENTETID_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [søk]);

  const gjeldende = søk !== "" && state?.søketekst === søk ? state : undefined;

  return {
    treff: gjeldende?.treff ?? [],
    laster: søk !== "" && gjeldende === undefined,
    feil: gjeldende?.feil,
  };
}
