import { useCallback, useEffect, useState } from "react";

interface PeriodeMedFraOgMedDato {
  fraOgMedDato?: string | null;
}

interface PerioderState<T> {
  behandlingId: string;
  hentIndeks: number;
  data?: T[];
  error?: unknown;
}

export interface PerioderResult<T> {
  data?: T[];
  error?: unknown;
  laster: boolean;
  hentPåNytt: () => void;
}

type HentPerioder<T extends PeriodeMedFraOgMedDato> = (
  behandlingId: string,
  signal: AbortSignal
) => Promise<T[]>;

function sorterPerioderEtterFraOgMedDato<T extends PeriodeMedFraOgMedDato>(perioder: T[]): T[] {
  return [...perioder].sort((a, b) => {
    const fraA = a.fraOgMedDato ?? "";
    const fraB = b.fraOgMedDato ?? "";

    return fraA < fraB ? -1 : fraA > fraB ? 1 : 0;
  });
}

export function usePerioder<T extends PeriodeMedFraOgMedDato>(
  behandlingId: string,
  hent: HentPerioder<T>
): PerioderResult<T> {
  const [state, settState] = useState<PerioderState<T>>({
    behandlingId,
    hentIndeks: -1,
  });
  const [hentIndeks, settHentIndeks] = useState(0);

  const hentPåNytt = useCallback(() => settHentIndeks((indeks) => indeks + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    void hent(behandlingId, signal).then(
      (data) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, data: sorterPerioderEtterFraOgMedDato(data) });
      },
      (error: unknown) => {
        if (signal.aborted) return;
        settState({ behandlingId, hentIndeks, error });
      }
    );

    return () => controller.abort();
  }, [behandlingId, hent, hentIndeks]);

  const harGjeldendeState = state.behandlingId === behandlingId && state.hentIndeks === hentIndeks;

  return {
    data: state.behandlingId === behandlingId ? state.data : undefined,
    error: state.behandlingId === behandlingId ? state.error : undefined,
    laster: Boolean(behandlingId) && !harGjeldendeState,
    hentPåNytt,
  };
}
