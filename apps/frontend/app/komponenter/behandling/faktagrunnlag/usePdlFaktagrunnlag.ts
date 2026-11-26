import { useCallback, useEffect, useState } from "react";
import {
  hentFalskIdentitet,
  hentMedlemskapFaktagrunnlag,
  innhentFaktagrunnlagFraPdl,
} from "~/api/generated/sdk.gen";
import { useApiKall } from "../vilkår/felles/useApiKall";

interface HentetState<Respons> {
  behandlingId: string;
  hentIndeks: number;
  respons?: Respons;
  error?: unknown;
}

type InnhentOptions = Parameters<typeof innhentFaktagrunnlagFraPdl>[0];

const hentMedlemskapsgrunnlagFraBackend = (behandlingId: string, signal: AbortSignal) =>
  hentMedlemskapFaktagrunnlag({ path: { behandlingId }, signal, throwOnError: true }).then(
    ({ data }) => data
  );

const hentFalskIdentitetFraBackend = (behandlingId: string, signal: AbortSignal) =>
  hentFalskIdentitet({ path: { behandlingId }, signal, throwOnError: true }).then(
    ({ data }) => data
  );

function useHentetFaktagrunnlag<Respons>(
  behandlingId: string,
  hentIndeks: number,
  hentRespons: (behandlingId: string, signal: AbortSignal) => Promise<Respons>
) {
  const [state, settState] = useState<HentetState<Respons>>({ behandlingId, hentIndeks: -1 });

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    void hentRespons(behandlingId, signal).then(
      (respons) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, respons });
      },
      (error: unknown) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, error });
      }
    );

    return () => controller.abort();
  }, [behandlingId, hentIndeks, hentRespons]);

  const harGjeldendeState = state.behandlingId === behandlingId && state.hentIndeks === hentIndeks;
  const respons = state.behandlingId === behandlingId ? state.respons : undefined;

  return {
    respons,
    error: state.behandlingId === behandlingId ? state.error : undefined,
    laster: Boolean(behandlingId) && !harGjeldendeState && respons === undefined,
  };
}

export function usePdlFaktagrunnlag(behandlingId: string) {
  const [hentIndeks, settHentIndeks] = useState(0);
  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  const medlemskapHentet = useHentetFaktagrunnlag(
    behandlingId,
    hentIndeks,
    hentMedlemskapsgrunnlagFraBackend
  );
  const falskIdentitetHentet = useHentetFaktagrunnlag(
    behandlingId,
    hentIndeks,
    hentFalskIdentitetFraBackend
  );

  const innhentFraPdlKall = useCallback(
    (options: InnhentOptions) => innhentFaktagrunnlagFraPdl({ ...options, throwOnError: true }),
    []
  );
  const innhentFraPdl = useApiKall(innhentFraPdlKall);

  return {
    medlemskapFaktagrunnlagTilstand: {
      medlemskapFaktagrunnlag: medlemskapHentet.respons,
      error: medlemskapHentet.error,
      laster: medlemskapHentet.laster,
      hentPåNytt,
      innhentFraPdl,
    },
    falskIdentitetTilstand: {
      harFalskIdentitet: falskIdentitetHentet.respons?.harFalskIdentitet,
      error: falskIdentitetHentet.error,
    },
  };
}
