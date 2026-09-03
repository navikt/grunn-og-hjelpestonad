import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { BehandlingEndring } from "~/types/endringshistorikk";
import { lyttPåEndringshistorikk } from "~/utils/endringshistorikkEvent";

interface EndringshistorikkState {
  endringshistorikk: BehandlingEndring[] | null;
  laster: boolean;
}

export function useHentEndringshistorikk(behandlingId: string | undefined) {
  const [state, settState] = useState<EndringshistorikkState>({
    endringshistorikk: null,
    laster: true,
  });

  const hent = useCallback(async () => {
    if (!behandlingId) {
      settState((prev) => ({ ...prev, laster: false }));
      return;
    }

    const response = await apiCall<BehandlingEndring[]>(
      `/endringshistorikk/${behandlingId}`
    );

    settState({
      endringshistorikk: response.data ?? null,
      laster: false,
    });
  }, [behandlingId]);

  useEffect(() => {
    settState((prev) => ({ ...prev, laster: true }));
    hent();
  }, [hent]);

  useEffect(() => {
    return lyttPåEndringshistorikk(hent);
  }, [hent]);

  return state;
}
