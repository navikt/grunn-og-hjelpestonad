import { useState } from "react";
import { feilmeldingFra } from "./feilmeldingFra";
import type { ApiKallCallbacks } from "./useVilkårApiKall";
import type { PerioderResult } from "./usePerioder";

export interface VilkårSletting<T> {
  slett: (periode: T) => void;
  sletterId?: string;
  feil?: string;
  nullstillFeil: () => void;
}

export function useSlettVilkårPeriode<T extends { id: string }>(
  slettPeriode: (vilkårPeriodeId: string, callbacks: ApiKallCallbacks) => void,
  perioder: PerioderResult<T>
): VilkårSletting<T> {
  const [sletterId, settSletterId] = useState<string | undefined>(undefined);
  const [feil, settFeil] = useState<string | undefined>(undefined);

  const slett = (periode: T) => {
    settSletterId(periode.id);
    settFeil(undefined);

    slettPeriode(periode.id, {
      onSuccess: () => perioder.hentPåNytt(),
      onError: (error) => settFeil(feilmeldingFra(error, "Kunne ikke slette perioden.")),
      onSettled: () => settSletterId(undefined),
    });
  };

  return {
    slett,
    sletterId,
    feil,
    nullstillFeil: () => settFeil(undefined),
  };
}
