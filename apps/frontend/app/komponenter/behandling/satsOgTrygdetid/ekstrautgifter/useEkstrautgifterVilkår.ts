import {
  hentEkstrautgifterPerioder,
  lagreEkstrautgifterPeriode,
  slettEkstrautgifterPeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårEkstrautgifterResponse } from "~/api/generated/types.gen";
import { useVilkårApiKall } from "~/komponenter/behandling/vilkår/felles/useVilkårApiKall";

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
  const lagreKall = (options: LagreEkstrautgifterOptions) =>
    lagreEkstrautgifterPeriode({ ...options, throwOnError: true });
  const slettKall = (options: SlettEkstrautgifterOptions) =>
    slettEkstrautgifterPeriode({ ...options, throwOnError: true });

  return {
    lagreEkstrautgifter: useVilkårApiKall(lagreKall),
    slettEkstrautgifter: useVilkårApiKall(slettKall),
  };
}
