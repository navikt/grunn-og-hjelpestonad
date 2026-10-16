import { useEffect, useState } from "react";
import { hentJournalPostForBrukerId } from "~/api/generated/sdk.gen";
import type { DokumentinfoResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface DokumenterState {
  dokumenter: DokumentinfoResponse[] | null;
  melding: string | null;
  laster: boolean;
}

export function useHentDokumenter(fagsakPersonId: string | undefined) {
  const [state, settState] = useState<DokumenterState>({
    dokumenter: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentDokumenter = async () => {
      if (!fagsakPersonId) {
        settState((prev) => ({
          ...prev,
          melding: "Mangler fagsakId",
          laster: false,
        }));
        return;
      }

      settState((prev) => ({ ...prev, melding: null, laster: true }));

      try {
        const { data } = await hentJournalPostForBrukerId({
          body: { fagsakPersonId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;
        settState((prev) => ({
          ...prev,
          dokumenter: data,
          melding: null,
          laster: false,
        }));
      } catch (feil: unknown) {
        if (signal.aborted) return;
        settState((prev) => ({
          ...prev,
          dokumenter: null,
          melding: feilmeldingFra(feil, "Vi kunne ikke hente dokumenter. Prøv igjen senere."),
          laster: false,
        }));
      }
    };

    void hentDokumenter();

    return () => controller.abort();
  }, [fagsakPersonId]);

  return state;
}
