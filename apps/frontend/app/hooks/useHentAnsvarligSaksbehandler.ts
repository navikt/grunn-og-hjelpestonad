import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { AnsvarligSaksbehandlerResponse } from "~/types/saksbehandler";

export const ANSVARLIG_SAKSBEHANDLER_STI = "/oppgave/ansvarlig-saksbehandler";

export function useHentAnsvarligSaksbehandler(behandlingId: string | undefined) {
  const [ansvarligSaksbehandler, settAnsvarligSaksbehandler] =
    useState<AnsvarligSaksbehandlerResponse | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);
  const [manglerTilgang, settManglerTilgang] = useState(false);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  useEffect(() => {
    if (!behandlingId) return;

    let avbrutt = false;

    void apiCall<AnsvarligSaksbehandlerResponse>(ANSVARLIG_SAKSBEHANDLER_STI, {
      method: "POST",
      body: JSON.stringify({ behandlingId }),
    }).then((respons) => {
      if (avbrutt) return;

      settAnsvarligSaksbehandler(respons.data ?? null);
      settManglerTilgang(respons.httpStatus === 403);
      settFullførtHenting(gjeldendeHenting);
    });

    return () => {
      avbrutt = true;
    };
  }, [behandlingId, gjeldendeHenting]);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  return {
    ansvarligSaksbehandler,
    laster,
    manglerTilgang,
    hentPåNytt,
  };
}
