import { useState, useEffect, useCallback } from "react";
import { apiCall } from "~/api/backend";
import { erGyldigFagsakPersonId, erGyldigSøkestreng } from "~/utils/utils";

interface UseSøkReturn {
  søk: string;
  søkeresultat: Søkeresultat | null;
  søker: boolean;
  feilmelding: string | null;
  settSøk: (value: string) => void;
  tilbakestillSøk: () => void;
}

export interface Søkeresultat {
  navn: string;
  fødselsdato: string | null;
  personident: string;
  fagsakPersonId: string;
  harFagsak: boolean;
}

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

    let avbrutt = false;

    const erFagsakPersonId = erGyldigFagsakPersonId(trimmetSøk);
    const body = erFagsakPersonId ? { fagsakPersonId: trimmetSøk } : { personident: trimmetSøk };

    void apiCall<Søkeresultat>(`/sok/person`, {
      method: "POST",
      body: JSON.stringify(body),
    }).then((response) => {
      if (avbrutt) return;

      settFullførtSøk({
        søkestreng: trimmetSøk,
        resultat: response.data ?? null,
        feilmelding: response.data ? null : (response.melding ?? "Kunne ikke utføre søket"),
      });
    });

    return () => {
      avbrutt = true;
    };
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
