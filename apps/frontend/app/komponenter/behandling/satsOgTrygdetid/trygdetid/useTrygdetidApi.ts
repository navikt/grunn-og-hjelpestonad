import {
  hentTrygdetidPerioder,
  lagreTrygdetidPeriode,
  slettTrygdetidPeriode,
} from "~/api/generated/sdk.gen";
import type { TrygdetidResponse } from "~/api/generated/types.gen";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
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
  const { hentBehandlingPåNytt } = useBehandlingContext();

  const lagreKall = async (options: LagreTrygdetidOptions) => {
    const lagreSvar = await lagreTrygdetidPeriode({ ...options, throwOnError: true });
    await hentBehandlingPåNytt();
    return lagreSvar;
  };
  const slettKall = async (options: SlettTrygdetidOptions) => {
    const slettSvar = await slettTrygdetidPeriode({ ...options, throwOnError: true });
    await hentBehandlingPåNytt();
    return slettSvar;
  };

  return {
    lagreTrygdetid: useApiKall(lagreKall),
    slettTrygdetid: useApiKall(slettKall),
  };
}
