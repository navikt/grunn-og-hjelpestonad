import { useCallback } from "react";
import {
  hentMedlemskapPerioder,
  lagreMedlemskapPeriode,
  slettMedlemskapPeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårMedlemskapResponse } from "~/api/generated/types.gen";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useApiKall } from "../felles/useApiKall";

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
  const { hentBehandlingPåNytt } = useBehandlingContext();

  const lagreKall = useCallback(
    async (options: LagreMedlemskapOptions) => {
      const lagreSvar = await lagreMedlemskapPeriode({ ...options, throwOnError: true });
      await hentBehandlingPåNytt();
      return lagreSvar;
    },
    [hentBehandlingPåNytt]
  );
  const slettKall = useCallback(
    async (options: SlettMedlemskapOptions) => {
      const slettSvar = await slettMedlemskapPeriode({ ...options, throwOnError: true });
      await hentBehandlingPåNytt();
      return slettSvar;
    },
    [hentBehandlingPåNytt]
  );

  return {
    lagre: useApiKall(lagreKall),
    slett: useApiKall(slettKall),
  };
}
