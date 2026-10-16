import { useCallback, useEffect, useState } from "react";
import { hentAnsvarligSaksbehandler as hentAnsvarligSaksbehandlerApi } from "~/api/generated/sdk.gen";
import type { AnsvarligSaksbehandlerResponse } from "~/api/generated/types.gen";

export function useHentAnsvarligSaksbehandler(behandlingId: string | undefined) {
  const [ansvarligSaksbehandler, settAnsvarligSaksbehandler] =
    useState<AnsvarligSaksbehandlerResponse | null>(null);
  const [hentIndeks, settHentIndeks] = useState(0);
  const [fullførtHenting, settFullførtHenting] = useState<string | null>(null);

  const gjeldendeHenting = `${behandlingId}#${hentIndeks}`;
  const laster = Boolean(behandlingId) && fullførtHenting !== gjeldendeHenting;

  useEffect(() => {
    if (!behandlingId) return;

    const controller = new AbortController();
    const { signal } = controller;

    const hent = async () => {
      try {
        const { data } = await hentAnsvarligSaksbehandlerApi({
          body: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;

        settAnsvarligSaksbehandler(data);
      } catch {
        if (signal.aborted) return;
        settAnsvarligSaksbehandler(null);
      } finally {
        if (!signal.aborted) settFullførtHenting(gjeldendeHenting);
      }
    };

    void hent();
    return () => controller.abort();
  }, [behandlingId, gjeldendeHenting]);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  return {
    ansvarligSaksbehandler,
    laster,
    hentPåNytt,
  };
}
