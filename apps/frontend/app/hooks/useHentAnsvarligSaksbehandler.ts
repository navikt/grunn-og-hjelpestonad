import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { AnsvarligSaksbehandlerResponse } from "~/types/saksbehandler";

export function useHentAnsvarligSaksbehandler(behandlingId: string | undefined) {
  const [ansvarligSaksbehandler, settAnsvarligSaksbehandler] =
    useState<AnsvarligSaksbehandlerResponse | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  useEffect(() => {
    if (!behandlingId) return;

    let avbrutt = false;

    void apiCall<AnsvarligSaksbehandlerResponse>(`/oppgave/ansvarlig-saksbehandler`, {
      method: "POST",
      body: JSON.stringify({ behandlingId }),
    }).then((respons) => {
      if (avbrutt) return;

      settAnsvarligSaksbehandler(respons.data ?? null);
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
    hentPåNytt,
  };
}
