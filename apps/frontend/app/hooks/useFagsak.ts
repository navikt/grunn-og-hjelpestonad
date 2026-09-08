import { useEffect, useState } from "react";
import { type ApiResponse, apiCall } from "~/api/backend";
import { erGyldigFagsakPersonId, erGyldigPersonident } from "~/utils/utils";

export type StønadType = "BARNETILSYN" | "SKOLEPENGER";

export interface FagsakDto {
  id: string;
  fagsakPersonId: string;
  personident: string;
  stønadstype: StønadType;
  eksternId?: number;
}

export interface FagsakRequest {
  personident?: string;
  fagsakPersonId?: string;
  stønadstype: StønadType;
}

interface FagsakState {
  fagsakPersonId?: string;
  fagsak: FagsakDto | null;
  melding: string | null;
  laster: boolean;
}

export async function hentEllerOpprettFagsak(søkestreng: string): Promise<ApiResponse<FagsakDto>> {
  // TODO: Refaktorer - kanskje dele opp i to funksjoner
  const id = søkestreng.trim();

  if (!erGyldigFagsakPersonId(id) && !erGyldigPersonident(id)) {
    return {
      melding: "Feil ved validering av fagsakPersonId/personident",
    };
  }

  const request: FagsakRequest = erGyldigFagsakPersonId(id)
    ? { fagsakPersonId: id, stønadstype: "BARNETILSYN" }
    : { personident: id, stønadstype: "BARNETILSYN" };

  return apiCall(`/fagsak`, {
    method: "POST",
    body: JSON.stringify(request),
  });
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
      const response = await hentEllerOpprettFagsak(fagsakPersonId);
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
