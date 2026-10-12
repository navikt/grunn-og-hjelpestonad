import { useEffect, useState } from "react";
import type { ApiResponse } from "~/api/backend";
import { hentFagsak as hentFagsakApi } from "~/api/generated/sdk.gen";
import type { FagsakRequest, FagsakResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { erGyldigFagsakPersonId } from "~/utils/utils";

export const STØNADSTYPE: FagsakRequest["stønadstype"] = "BARNETILSYN";

interface FagsakState {
  fagsakPersonId?: string;
  fagsak: FagsakResponse | null;
  melding: string | null;
  laster: boolean;
}

// Bare lesing. Fagsaken opprettes eksplisitt fra søket, se useOpprettFagsak.
async function hentFagsakForPerson(fagsakPersonId: string): Promise<ApiResponse<FagsakResponse>> {
  if (!erGyldigFagsakPersonId(fagsakPersonId)) {
    return { melding: "Ugyldig fagsakPersonId" };
  }

  try {
    const { data } = await hentFagsakApi({
      path: { fagsakPersonId, stonadstype: STØNADSTYPE },
      throwOnError: true,
    });
    return { data };
  } catch (feil) {
    return { melding: feilmeldingFra(feil, "Fagsak ikke funnet") };
  }
}

export const useFagsak = (fagsakPersonId: string | undefined) => {
  const [state, settState] = useState<FagsakState>({
    fagsak: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    if (!fagsakPersonId) return;

    let avbrutt = false;

    const hentFagsak = async () => {
      const response = await hentFagsakForPerson(fagsakPersonId);
      if (avbrutt) return;

      const fagsak = response.data ?? null;

      if (fagsak) {
        settState({ fagsakPersonId, fagsak, melding: null, laster: false });
      } else {
        settState({
          fagsakPersonId,
          fagsak: null,
          melding: response.melding || "Fagsak ikke funnet",
          laster: false,
        });
      }
    };

    hentFagsak();

    return () => {
      avbrutt = true;
    };
  }, [fagsakPersonId]);

  if (!fagsakPersonId) {
    return {
      fagsak: null,
      melding: null,
      laster: false,
    };
  }

  if (state.fagsakPersonId !== fagsakPersonId) {
    return {
      fagsak: null,
      melding: null,
      laster: true,
    };
  }

  return {
    fagsak: state.fagsak,
    melding: state.melding,
    laster: state.laster,
  };
};
