import { useState, useCallback } from "react";
import { useNavigate } from "react-router";
import type { Søkeresultat } from "./useSøk";
import { hentEllerOpprettFagsak } from "./useFagsak";

interface OpprettFagsak {
  opprettFagsak: (søkeresultat: Søkeresultat) => Promise<void>;
  oppretter: boolean;
  opprettFeilmelding: string | null;
}

export const useOpprettFagsak = (): OpprettFagsak => {
  const navigate = useNavigate();
  const [oppretter, settOppretter] = useState(false);
  const [opprettFeilmelding, settOpprettFeilmelding] = useState<string | null>(null);

  const opprettFagsak = useCallback(
    async (søkeresultat: Søkeresultat) => {
      settOppretter(true);
      settOpprettFeilmelding(null);

      const response = await hentEllerOpprettFagsak(søkeresultat.personident);
      const fagsak = response.data;

      if (fagsak?.fagsakPersonId) {
        navigate(`/person/${fagsak.fagsakPersonId}/behandlingsoversikt`);
        return;
      }

      settOpprettFeilmelding(response.melding || "Kunne ikke opprette fagsak.");
      settOppretter(false);
    },
    [navigate]
  );

  return {
    opprettFagsak,
    oppretter,
    opprettFeilmelding,
  };
};
