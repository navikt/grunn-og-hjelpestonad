import { useState, useCallback } from "react";
import { useNavigate } from "react-router";
import { opprettFagsak as opprettFagsakApi } from "~/api/generated/sdk.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import type { Søkeresultat } from "./useSøk";
import { STØNADSTYPE } from "./useFagsak";

interface OpprettFagsak {
  opprettFagsak: (søkeresultat: Søkeresultat) => Promise<boolean>;
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

      try {
        const { data: fagsak } = await opprettFagsakApi({
          body: { personident: søkeresultat.personident, stønadstype: STØNADSTYPE },
          throwOnError: true,
        });
        await navigate(`/person/${fagsak.fagsakPersonId}/behandlingsoversikt`);
        return true;
      } catch (feil) {
        settOpprettFeilmelding(feilmeldingFra(feil, "Kunne ikke opprette fagsak."));
        return false;
      } finally {
        settOppretter(false);
      }
    },
    [navigate]
  );

  return {
    opprettFagsak,
    oppretter,
    opprettFeilmelding,
  };
};
