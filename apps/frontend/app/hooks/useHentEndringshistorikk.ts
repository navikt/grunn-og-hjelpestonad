import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { BehandlingEndring } from "~/types/endringshistorikk";
import { lyttPåEndringshistorikk } from "~/utils/endringshistorikkEvent";

export function useHentEndringshistorikk(behandlingId: string | undefined) {
  const [endringshistorikk, settEndringshistorikk] = useState<BehandlingEndring[] | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  useEffect(() => {
    if (!behandlingId) return;

    let avbrutt = false;

    void apiCall<BehandlingEndring[]>(`/endringshistorikk/${behandlingId}`).then((respons) => {
      if (avbrutt) return;

      settEndringshistorikk(respons.data ?? null);
      settFullførtHenting(gjeldendeHenting);
    });

    return () => {
      avbrutt = true;
    };
  }, [behandlingId, gjeldendeHenting]);

  useEffect(() => {
    return lyttPåEndringshistorikk(hentPåNytt);
  }, [hentPåNytt]);

  return { endringshistorikk, laster };
}
