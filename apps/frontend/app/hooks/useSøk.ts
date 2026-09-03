import { useState, useEffect, useCallback } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
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

export const useSøk = (): UseSøkReturn => {
  const [søk, settSøk] = useState<string>("");
  const [søkeresultat, settSøkeresultat] = useState<Søkeresultat | null>(null);
  const [søker, settSøker] = useState(false);
  const [feilmelding, settFeilmelding] = useState<string | null>(null);

  const utførSøk = useCallback(async (søkestreng: string) => {
    const søkPerson = (søkestreng: string): Promise<ApiResponse<Søkeresultat>> => {
      const erFagsakPersonId = erGyldigFagsakPersonId(søkestreng);
      const body = erFagsakPersonId ? { fagsakPersonId: søkestreng } : { personident: søkestreng };

      return apiCall(`/sok/person`, {
        method: "POST",
        body: JSON.stringify(body),
      });
    };

    settSøker(true);
    settFeilmelding(null);
    settSøkeresultat(null);

    const response = await søkPerson(søkestreng);

    if (response.data) {
      settSøkeresultat(response.data);
    } else {
      settFeilmelding(response.melding ?? "Kunne ikke utføre søket");
    }

    settSøker(false);
  }, []);

  const tilbakestillSøk = useCallback(() => {
    settSøk("");
    settSøkeresultat(null);
    settFeilmelding(null);
  }, []);

  useEffect(() => {
    const trimmetSøk = søk.trim();

    if (erGyldigSøkestreng(trimmetSøk)) {
      utførSøk(trimmetSøk);
    } else if (trimmetSøk === "") {
      settSøkeresultat(null);
      settFeilmelding(null);
    }
  }, [søk, utførSøk]);

  return {
    søk,
    søkeresultat,
    søker,
    feilmelding,
    settSøk,
    tilbakestillSøk,
  };
};
