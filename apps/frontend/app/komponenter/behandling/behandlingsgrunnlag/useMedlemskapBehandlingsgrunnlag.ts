import { useCallback, useEffect, useState } from "react";
import {
  hentMedlemskapBehandlingsgrunnlag,
  innhentBehandlingsgrunnlagFraPdl,
} from "~/api/generated/sdk.gen";
import type { MedlemskapBehandlingsgrunnlagResponse } from "~/api/generated/types.gen";
import { useVilkårApiKall } from "../vilkår/felles/useVilkårApiKall";

interface GrunnlagState {
  behandlingId: string;
  hentIndeks: number;
  medlemskapBehandlingsgrunnlag?: MedlemskapBehandlingsgrunnlagResponse;
  error?: unknown;
}

type InnhentOptions = Parameters<typeof innhentBehandlingsgrunnlagFraPdl>[0];

export function useMedlemskapBehandlingsgrunnlag(behandlingId: string) {
  const [state, settState] = useState<GrunnlagState>({ behandlingId, hentIndeks: -1 });
  const [hentIndeks, settHentIndeks] = useState(0);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    void hentMedlemskapBehandlingsgrunnlag({
      path: { behandlingId },
      signal,
      throwOnError: true,
    }).then(
      ({ data: medlemskapBehandlingsgrunnlag }) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, medlemskapBehandlingsgrunnlag });
      },
      (error: unknown) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, error });
      }
    );

    return () => controller.abort();
  }, [behandlingId, hentIndeks]);

  const innhentFraPdlKall = useCallback(
    (options: InnhentOptions) => innhentBehandlingsgrunnlagFraPdl({ ...options, throwOnError: true }),
    []
  );
  const innhentFraPdl = useVilkårApiKall(innhentFraPdlKall);

  const harGjeldendeState = state.behandlingId === behandlingId && state.hentIndeks === hentIndeks;
  const medlemskapBehandlingsgrunnlag =
    state.behandlingId === behandlingId ? state.medlemskapBehandlingsgrunnlag : undefined;

  return {
    medlemskapBehandlingsgrunnlag,
    error: state.behandlingId === behandlingId ? state.error : undefined,
    laster:
      Boolean(behandlingId) &&
      !harGjeldendeState &&
      medlemskapBehandlingsgrunnlag === undefined,
    hentPåNytt,
    innhentFraPdl,
  };
}
