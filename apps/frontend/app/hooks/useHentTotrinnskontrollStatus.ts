import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { TotrinnskontrollResponse } from "~/types/totrinnskontroll";

export const useHentTotrinnskontrollStatus = (behandlingId: string | undefined) => {
  const [totrinnskontrollStatus, settTotrinnskontrollStatus] =
    useState<TotrinnskontrollResponse | null>(null);
  const [laster, settLaster] = useState(true);

  const hentStatus = useCallback(async () => {
    if (!behandlingId) return;

    settLaster(true);
    try {
      const respons = await apiCall<TotrinnskontrollResponse>(
        `/beslutter/totrinnskontroll-status/${behandlingId}`
      );
      if (respons.data) {
        settTotrinnskontrollStatus(respons.data);
      }
    } finally {
      settLaster(false);
    }
  }, [behandlingId]);

  useEffect(() => {
    hentStatus();
  }, [hentStatus]);

  return {
    totrinnskontrollStatus,
    laster,
    hentPåNytt: hentStatus,
  };
};
