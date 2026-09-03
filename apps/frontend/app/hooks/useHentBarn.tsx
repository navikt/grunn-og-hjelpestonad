import { useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";

export interface Barn {
  id: string;
  personIdent: string;
  navn: string;
  fødselsdato: string;
  hentetTidspunkt: string;
}

interface HentBarnState {
  barn: Barn[];
  melding: string | null;
  laster: boolean;
}

export interface HentBarnRequest {
  personIdent: string;
  behandlingId: string | undefined;
}

export function useHentBarn(request: HentBarnRequest) {
  const [state, settState] = useState<HentBarnState>({
    barn: [],
    melding: null,
    laster: true,
  });

  const { personIdent, behandlingId } = request;

  useEffect(() => {
    const hentBarn = async (req: HentBarnRequest): Promise<ApiResponse<Barn[]>> => {
      return apiCall(`/barn/hent`, {
        method: "POST",
        body: JSON.stringify(req),
      });
    };

    let avbrutt = false;

    const fetchBarn = async () => {
      if (avbrutt) return;

      if (!personIdent && !behandlingId) {
        settState((prev) => ({
          ...prev,
          barn: [],
          melding: null,
          laster: false,
        }));
        return;
      }

      settState((prev) => ({ ...prev, melding: null, laster: true }));

      const response = await hentBarn({ personIdent, behandlingId });

      if (avbrutt) return;

      if (response.data) {
        settState((prev) => ({
          ...prev,
          barn: response.data ?? [],
          laster: false,
        }));
      } else {
        settState((prev) => ({
          ...prev,
          barn: [],
          melding: response.melding ?? "Kunne ikke hente barn",
          laster: false,
        }));
      }
    };

    fetchBarn();

    return () => {
      avbrutt = true;
    };
  }, [personIdent, behandlingId]);

  return state;
}
