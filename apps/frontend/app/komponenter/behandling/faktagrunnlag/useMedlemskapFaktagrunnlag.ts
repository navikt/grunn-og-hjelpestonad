import { useCallback, useEffect, useState } from "react";
import {
  hentMedlemskapFaktagrunnlag,
  innhentFaktagrunnlagFraPdl,
} from "~/api/generated/sdk.gen";
import type { MedlemskapFaktagrunnlagResponse } from "~/api/generated/types.gen";
import { useVilkårApiKall } from "../vilkår/felles/useVilkårApiKall";

interface GrunnlagState {
  behandlingId: string;
  hentIndeks: number;
  medlemskapFaktagrunnlag?: MedlemskapFaktagrunnlagResponse;
  error?: unknown;
}

type InnhentOptions = Parameters<typeof innhentFaktagrunnlagFraPdl>[0];

export function useMedlemskapFaktagrunnlag(behandlingId: string) {
  const [state, settState] = useState<GrunnlagState>({ behandlingId, hentIndeks: -1 });
  const [hentIndeks, settHentIndeks] = useState(0);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    void hentMedlemskapFaktagrunnlag({
      path: { behandlingId },
      signal,
      throwOnError: true,
    }).then(
      ({ data: medlemskapFaktagrunnlag }) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, medlemskapFaktagrunnlag });
      },
      (error: unknown) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, error });
      }
    );

    return () => controller.abort();
  }, [behandlingId, hentIndeks]);

  const innhentFraPdlKall = useCallback(
    (options: InnhentOptions) => innhentFaktagrunnlagFraPdl({ ...options, throwOnError: true }),
    []
  );
  const innhentFraPdl = useVilkårApiKall(innhentFraPdlKall);

  const harGjeldendeState = state.behandlingId === behandlingId && state.hentIndeks === hentIndeks;
  const medlemskapFaktagrunnlag =
    state.behandlingId === behandlingId ? state.medlemskapFaktagrunnlag : undefined;

  return {
    medlemskapFaktagrunnlag,
    error: state.behandlingId === behandlingId ? state.error : undefined,
    laster:
      Boolean(behandlingId) &&
      !harGjeldendeState &&
      medlemskapFaktagrunnlag === undefined,
    hentPåNytt,
    innhentFraPdl,
  };
}
