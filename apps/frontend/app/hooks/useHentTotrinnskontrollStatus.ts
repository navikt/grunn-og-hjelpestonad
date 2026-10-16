import { useCallback, useEffect, useState } from "react";
import { hentTotrinnskontrollStatus as hentTotrinnskontrollStatusApi } from "~/api/generated/sdk.gen";
import type { TotrinnskontrollStatusResponse } from "~/api/generated/types.gen";
import type { TotrinnskontrollResponse } from "~/types/totrinnskontroll";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

export const useHentTotrinnskontrollStatus = (behandlingId: string | undefined) => {
  const [totrinnskontrollStatus, settTotrinnskontrollStatus] =
    useState<TotrinnskontrollStatusResponse | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  useEffect(() => {
    if (!behandlingId) return;

    const controller = new AbortController();
    const { signal } = controller;

    const hentStatus = async () => {
      try {
        const { data } = await hentTotrinnskontrollStatusApi({
          path: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;

        settTotrinnskontrollStatus(data);
        settFullførtHenting(gjeldendeHenting);
      } catch (feil: unknown) {
        if (signal.aborted) return;
        console.error(
          "Kunne ikke hente totrinnskontrollstatus:",
          feilmeldingFra(feil, "Kunne ikke hente totrinnskontrollstatus")
        );
        settFullførtHenting(gjeldendeHenting);
      }
    };

    void hentStatus();

    return () => controller.abort();
  }, [behandlingId, gjeldendeHenting]);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  return {
    totrinnskontrollStatus: totrinnskontrollStatus as TotrinnskontrollResponse | null,
    laster,
    hentPåNytt,
  };
};
