import { useCallback } from "react";
import {
  hentInstitusjonPerioder,
  lagreInstitusjonPeriode,
  slettInstitusjonPeriode,
} from "~/api/generated/sdk.gen";
import type { VilkårInstitusjonResponse } from "~/api/generated/types.gen";
import { useVilkårApiKall } from "../felles/useVilkårApiKall";

export async function hentInstitusjonPerioderFraBackend(
  behandlingId: string,
  signal: AbortSignal
): Promise<VilkårInstitusjonResponse[]> {
  const { data } = await hentInstitusjonPerioder({
    path: { behandlingId },
    signal,
    throwOnError: true,
  });
  return data;
}

type LagreInstitusjonOptions = Parameters<typeof lagreInstitusjonPeriode>[0];
type SlettInstitusjonOptions = Parameters<typeof slettInstitusjonPeriode>[0];

export function useInstitusjonVilkårApi() {
  const lagreKall = useCallback(
    (options: LagreInstitusjonOptions) =>
      lagreInstitusjonPeriode({ ...options, throwOnError: true }),
    []
  );
  const slettKall = useCallback(
    (options: SlettInstitusjonOptions) =>
      slettInstitusjonPeriode({ ...options, throwOnError: true }),
    []
  );

  return {
    lagre: useVilkårApiKall(lagreKall),
    slett: useVilkårApiKall(slettKall),
  };
}
