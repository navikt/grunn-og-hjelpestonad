import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { AnsvarligSaksbehandlerDto } from "~/types/saksbehandler";

interface AnsvarligSaksbehandlerState {
  ansvarligSaksbehandler: AnsvarligSaksbehandlerDto | null;
  laster: boolean;
}

export function useHentAnsvarligSaksbehandler(behandlingId: string | undefined) {
  const [state, settState] = useState<AnsvarligSaksbehandlerState>({
    ansvarligSaksbehandler: null,
    laster: true,
  });

  const hentAnsvarligSaksbehandler = useCallback(async () => {
    if (!behandlingId) {
      settState((prev) => ({ ...prev, laster: false }));
      return;
    }

    settState((prev) => ({ ...prev, laster: true }));

    const response = await apiCall<AnsvarligSaksbehandlerDto>(
      `/oppgave/ansvarlig-saksbehandler`,
      {
        method: "POST",
        body: JSON.stringify({ behandlingId }),
      }
    );

    if (response.data) {
      settState({
        ansvarligSaksbehandler: response.data,
        laster: false,
      });
    } else {
      settState({
        ansvarligSaksbehandler: null,
        laster: false,
      });
    }
  }, [behandlingId]);

  useEffect(() => {
    hentAnsvarligSaksbehandler();
  }, [hentAnsvarligSaksbehandler]);

  return {
    ...state,
    hentPåNytt: hentAnsvarligSaksbehandler,
  };
}
