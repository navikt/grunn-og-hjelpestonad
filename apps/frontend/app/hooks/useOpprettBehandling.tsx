import { useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";

interface OpprettBehandlingResponse {
  behandlingId: string;
}

interface OpprettBehandling {
  opprettBehandling: (fagsakId: string) => Promise<string | undefined>;
  oppretter: boolean;
  opprettFeilmelding: string | null;
}

export const useOpprettBehandling = (): OpprettBehandling => {
  const [oppretter, settOppretter] = useState(false);
  const [opprettFeilmelding, settOpprettFeilmelding] = useState<string | null>(null);

  const opprettBehandling = async (fagsakId: string): Promise<string | undefined> => {
    const opprettBehandlingApi = (
      fagsakId: string
    ): Promise<ApiResponse<OpprettBehandlingResponse>> => {
      return apiCall(`/behandling/opprett`, {
        method: "POST",
        body: JSON.stringify({ fagsakId }),
      });
    };

    settOppretter(true);
    settOpprettFeilmelding(null);

    const response = await opprettBehandlingApi(fagsakId);

    if (!response.data && response.melding) {
      settOpprettFeilmelding(response.melding);
      settOppretter(false);
      return undefined;
    }

    settOppretter(false);
    return response.data?.behandlingId;
  };

  return {
    opprettBehandling,
    oppretter,
    opprettFeilmelding,
  };
};
