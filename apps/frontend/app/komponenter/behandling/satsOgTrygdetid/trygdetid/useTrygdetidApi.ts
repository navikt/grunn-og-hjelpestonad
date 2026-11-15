import {
  hentTrygdetidPerioder,
  lagreTrygdetidPeriode,
  slettTrygdetidPeriode,
} from "~/api/generated/sdk.gen";
import type { TrygdetidResponse } from "~/api/generated/types.gen";
import { useApiKall } from "~/komponenter/behandling/vilkår/felles/useApiKall";

export async function hentTrygdetidPerioderFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<TrygdetidResponse[]> {
  const { data } = await hentTrygdetidPerioder({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

type LagreTrygdetidOptions = Parameters<typeof lagreTrygdetidPeriode>[0];
type SlettTrygdetidOptions = Parameters<typeof slettTrygdetidPeriode>[0];

export function useTrygdetidApi() {
  const lagreKall = (options: LagreTrygdetidOptions) =>
    lagreTrygdetidPeriode({ ...options, throwOnError: true });
  const slettKall = (options: SlettTrygdetidOptions) =>
    slettTrygdetidPeriode({ ...options, throwOnError: true });

  return {
    lagreTrygdetid: useApiKall(lagreKall),
    slettTrygdetid: useApiKall(slettKall),
  };
}
