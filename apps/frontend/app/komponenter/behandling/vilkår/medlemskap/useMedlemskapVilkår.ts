import { useCallback } from "react";
import {
  hentMedlemskapPerioder,
  lagreMedlemskapPeriode,
  slettMedlemskapPeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårMedlemskapResponse } from "~/api/generated/types.gen";
import { useVilkårApiKall } from "../felles/useVilkårApiKall";

export async function hentMedlemskapPerioderFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<VilkårMedlemskapResponse[]> {
  const { data } = await hentMedlemskapPerioder({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

type LagreMedlemskapOptions = Parameters<typeof lagreMedlemskapPeriode>[0];
type SlettMedlemskapOptions = Parameters<typeof slettMedlemskapPeriode>[0];

export function useMedlemskapVilkårApi() {
  const lagreKall = useCallback(
    (options: LagreMedlemskapOptions) => lagreMedlemskapPeriode({ ...options, throwOnError: true }),
    []
  );
  const slettKall = useCallback(
    (options: SlettMedlemskapOptions) => slettMedlemskapPeriode({ ...options, throwOnError: true }),
    []
  );

  return {
    lagre: useVilkårApiKall(lagreKall),
    slett: useVilkårApiKall(slettKall),
  };
}
