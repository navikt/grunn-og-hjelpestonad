import { useCallback } from "react";
import {
  hentDiagnosePerioder,
  lagreDiagnosePeriode,
  slettDiagnosePeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårDiagnoseResponse } from "~/api/generated/types.gen";
import { useVilkårApiKall } from "../felles/useVilkårApiKall";

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
  const lagreKall = useCallback(
    (options: LagreDiagnoseOptions) => lagreDiagnosePeriode({ ...options, throwOnError: true }),
    []
  );
  const slettKall = useCallback(
    (options: SlettDiagnoseOptions) => slettDiagnosePeriode({ ...options, throwOnError: true }),
    []
  );

  return {
    lagre: useVilkårApiKall(lagreKall),
    slett: useVilkårApiKall(slettKall),
  };
}
