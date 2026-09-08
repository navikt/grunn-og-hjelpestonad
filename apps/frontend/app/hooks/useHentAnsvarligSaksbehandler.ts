import { useCallback, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
import type { AnsvarligSaksbehandlerDto } from "~/types/saksbehandler";

export function useHentAnsvarligSaksbehandler(behandlingId: string | undefined) {
  const [ansvarligSaksbehandler, settAnsvarligSaksbehandler] =
    useState<AnsvarligSaksbehandlerDto | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  useEffect(() => {
    if (!behandlingId) return;

    let avbrutt = false;

    void apiCall<AnsvarligSaksbehandlerDto>(`/oppgave/ansvarlig-saksbehandler`, {
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
