import { useState, useEffect, useCallback } from "react";
import { søkPerson, søkPersonMedFagsakPersonId } from "~/api/generated/sdk.gen";
import type { SøkeresultatResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { erGyldigFagsakPersonId, erGyldigSøkestreng } from "~/utils/utils";

interface UseSøkReturn {
  søk: string;
  søkeresultat: Søkeresultat | null;
  søker: boolean;
  feilmelding: string | null;
  settSøk: (value: string) => void;
  tilbakestillSøk: () => void;
}

export type Søkeresultat = SøkeresultatResponse;

interface FullførtSøk {
  søkestreng: string;
  resultat: Søkeresultat | null;
  feilmelding: string | null;
}

export const useSøk = (): UseSøkReturn => {
  const [søk, settSøk] = useState<string>("");
  const [fullførtSøk, settFullførtSøk] = useState<FullførtSøk | null>(null);

  const trimmetSøk = søk.trim();
  const skalSøke = erGyldigSøkestreng(trimmetSøk);
  const gjelderGjeldendeSøk = fullførtSøk !== null && fullførtSøk.søkestreng === trimmetSøk;

  useEffect(() => {
    if (!skalSøke) return;

    const controller = new AbortController();
    const { signal } = controller;

    const søkekall: Promise<{ data: Søkeresultat }> = erGyldigFagsakPersonId(trimmetSøk)
      ? søkPersonMedFagsakPersonId({
          path: { fagsakPersonId: trimmetSøk },
          signal,
          throwOnError: true,
        })
      : søkPerson({ body: { personident: trimmetSøk }, signal, throwOnError: true });

    void søkekall.then(
      ({ data }) => {
        if (signal.aborted) return;
        settFullførtSøk({ søkestreng: trimmetSøk, resultat: data, feilmelding: null });
      },
      (feil: unknown) => {
        if (signal.aborted) return;
        settFullførtSøk({
          søkestreng: trimmetSøk,
          resultat: null,
          feilmelding: feilmeldingFra(feil, "Kunne ikke utføre søket"),
        });
      }
    );

    return () => controller.abort();
  }, [trimmetSøk, skalSøke]);

  const tilbakestillSøk = useCallback(() => {
    settSøk("");
    settFullførtSøk(null);
  }, []);

  return {
    søk,
    søkeresultat: gjelderGjeldendeSøk ? fullførtSøk.resultat : null,
    søker: skalSøke && !gjelderGjeldendeSøk,
    feilmelding: gjelderGjeldendeSøk ? fullførtSøk.feilmelding : null,
    settSøk,
    tilbakestillSøk,
  };
};
