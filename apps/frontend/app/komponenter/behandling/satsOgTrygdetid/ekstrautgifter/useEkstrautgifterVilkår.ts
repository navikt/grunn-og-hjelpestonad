import {
  hentEkstrautgifterPerioder,
  lagreEkstrautgifterPeriode,
  slettEkstrautgifterPeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårEkstrautgifterResponse } from "~/api/generated/types.gen";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useApiKall } from "~/komponenter/behandling/vilkår/felles/useApiKall";

export async function hentEkstrautgifterPerioderFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<VilkårEkstrautgifterResponse[]> {
  const { data } = await hentEkstrautgifterPerioder({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

type LagreEkstrautgifterOptions = Parameters<typeof lagreEkstrautgifterPeriode>[0];
type SlettEkstrautgifterOptions = Parameters<typeof slettEkstrautgifterPeriode>[0];

export function useEkstrautgifterVilkårApi() {
  const { hentBehandlingPåNytt } = useBehandlingContext();

  const lagreKall = async (options: LagreEkstrautgifterOptions) => {
    const lagreSvar = await lagreEkstrautgifterPeriode({ ...options, throwOnError: true });
    await hentBehandlingPåNytt();
    return lagreSvar;
  };
  const slettKall = async (options: SlettEkstrautgifterOptions) => {
    const slettSvar = await slettEkstrautgifterPeriode({ ...options, throwOnError: true });
    await hentBehandlingPåNytt();
    return slettSvar;
  };

  return {
    lagreEkstrautgifter: useApiKall(lagreKall),
    slettEkstrautgifter: useApiKall(slettKall),
  };
}
