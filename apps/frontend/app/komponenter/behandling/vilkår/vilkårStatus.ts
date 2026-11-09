import type { VilkårDiagnoseResponse, VilkårMedlemskapResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "./felles/feilmeldingFra";
import type { PerioderResult } from "./felles/usePerioder";

export interface VilkårStatus {
  laster: boolean;
  feilmelding?: string;
}

export function lagVilkårStatus({
  medlemskapsPerioder,
  diagnosePerioder,
}: {
  medlemskapsPerioder: PerioderResult<VilkårMedlemskapResponse>;
  diagnosePerioder: PerioderResult<VilkårDiagnoseResponse>;
}): VilkårStatus {
  const periodeResultater = [medlemskapsPerioder, diagnosePerioder];
  const førsteFeil = periodeResultater.find((resultat) => resultat.error)?.error;

  return {
    laster: periodeResultater.some((resultat) => resultat.laster),
    feilmelding: førsteFeil
      ? feilmeldingFra(førsteFeil, "Kunne ikke hente vilkårsvurderinger")
      : undefined,
  };
}
