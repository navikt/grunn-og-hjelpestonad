import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { TotrinnskontrollResponse } from "~/types/totrinnskontroll";

export const useHentTotrinnskontrollStatus = (behandlingId: string | undefined) => {
  const [totrinnskontrollStatus, settTotrinnskontrollStatus] =
    useState<TotrinnskontrollResponse | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  useEffect(() => {
    if (!behandlingId) return;

    let avbrutt = false;

    void apiCall<TotrinnskontrollResponse>(
      `/beslutter/totrinnskontroll-status/${behandlingId}`
    ).then((respons) => {
      if (avbrutt) return;

      if (respons.data) {
        settTotrinnskontrollStatus(respons.data);
      }
      settFullførtHenting(gjeldendeHenting);
    });

    return () => {
      avbrutt = true;
    };
  }, [behandlingId, gjeldendeHenting]);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  return {
    totrinnskontrollStatus,
    laster,
    hentPåNytt,
  };
};
