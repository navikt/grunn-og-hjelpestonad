import { useCallback, useEffect, useState } from "react";
import { hentEndringshistorikk } from "~/api/generated/sdk.gen";
import type { BehandlingEndringResponse } from "~/api/generated/types.gen";
import { lyttPåEndringshistorikk } from "~/utils/endringshistorikkEvent";

export function useHentEndringshistorikk(behandlingId: string | undefined) {
  const [endringshistorikk, settEndringshistorikk] = useState<BehandlingEndringResponse[] | null>(
    null
  );
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  useEffect(() => {
    if (!behandlingId) return;

    const controller = new AbortController();
    const { signal } = controller;

    const hentHistorikk = async () => {
      try {
        const { data } = await hentEndringshistorikk({
          path: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;

        settEndringshistorikk(data);
        settFullførtHenting(gjeldendeHenting);
      } catch {
        if (signal.aborted) return;
        settEndringshistorikk(null);
        settFullførtHenting(gjeldendeHenting);
      }
    };

    void hentHistorikk();

    return () => controller.abort();
  }, [behandlingId, gjeldendeHenting]);

  useEffect(() => {
    return lyttPåEndringshistorikk(hentPåNytt);
  }, [hentPåNytt]);

  return { endringshistorikk, laster };
}
