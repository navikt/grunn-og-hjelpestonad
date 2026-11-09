import { hentPerioderMedOppfylteInngangsvilkår } from "~/api/generated/sdk.gen";
import type { PeriodeMedOppfylteInngangsvilkårResponse } from "~/api/generated/types.gen";
import { usePerioder, type PerioderResult } from "../vilkår/felles/usePerioder";

async function hentPerioderMedOppfylteInngangsvilkårFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<PeriodeMedOppfylteInngangsvilkårResponse[]> {
  const { data } = await hentPerioderMedOppfylteInngangsvilkår({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

export function useInngangsvilkårPerioder(
  behandlingId: string
): PerioderResult<PeriodeMedOppfylteInngangsvilkårResponse> {
  return usePerioder(behandlingId, hentPerioderMedOppfylteInngangsvilkårFraBackend);
}
