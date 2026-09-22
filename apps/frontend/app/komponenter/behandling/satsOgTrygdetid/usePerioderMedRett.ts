import { hentPerioderMedRett } from "~/api/generated/sdk.gen";
import type { PeriodeMedRettResponse } from "~/api/generated/types.gen";
import { usePerioder, type PerioderResult } from "../vilkår/felles/usePerioder";

async function hentPerioderMedRettFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<PeriodeMedRettResponse[]> {
  const { data } = await hentPerioderMedRett({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

export function usePerioderMedRett(behandlingId: string): PerioderResult<PeriodeMedRettResponse> {
  return usePerioder(behandlingId, hentPerioderMedRettFraBackend);
}
