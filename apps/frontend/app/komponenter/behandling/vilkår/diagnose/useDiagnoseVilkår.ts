import { useCallback } from "react";
import {
  hentDiagnosePerioder,
  lagreDiagnosePeriode,
  slettDiagnosePeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårDiagnoseResponse } from "~/api/generated/types.gen";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useApiKall } from "../felles/useApiKall";

export async function hentDiagnosePerioderFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<VilkårDiagnoseResponse[]> {
  const { data } = await hentDiagnosePerioder({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

type LagreDiagnoseOptions = Parameters<typeof lagreDiagnosePeriode>[0];
type SlettDiagnoseOptions = Parameters<typeof slettDiagnosePeriode>[0];

export function useDiagnoseVilkårApi() {
  const { hentBehandlingPåNytt } = useBehandlingContext();

  const lagreKall = useCallback(
    async (options: LagreDiagnoseOptions) => {
      const lagreSvar = await lagreDiagnosePeriode({ ...options, throwOnError: true });
      await hentBehandlingPåNytt();
      return lagreSvar;
    },
    [hentBehandlingPåNytt]
  );
  const slettKall = useCallback(
    async (options: SlettDiagnoseOptions) => {
      const slettSvar = await slettDiagnosePeriode({ ...options, throwOnError: true });
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
