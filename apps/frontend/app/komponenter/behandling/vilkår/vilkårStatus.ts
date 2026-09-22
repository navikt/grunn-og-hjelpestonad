import type {
  VilkårDiagnoseResponse,
  VilkårInstitusjonResponse,
  VilkårMedlemskapResponse,
} from "~/api/generated/types.gen";
import { feilmeldingFra } from "./felles/feilmeldingFra";
import type { PerioderResult } from "./felles/usePerioder";

export interface VilkårStatus {
  laster: boolean;
  feilmelding?: string;
}

export function lagVilkårStatus({
  medlemskapsPerioder,
  diagnosePerioder,
  institusjonsPerioder,
}: {
  medlemskapsPerioder: PerioderResult<VilkårMedlemskapResponse>;
  diagnosePerioder: PerioderResult<VilkårDiagnoseResponse>;
  institusjonsPerioder: PerioderResult<VilkårInstitusjonResponse>;
}): VilkårStatus {
  const periodeResultater = [medlemskapsPerioder, diagnosePerioder, institusjonsPerioder];
  const førsteFeil = periodeResultater.find((resultat) => resultat.error)?.error;

  return {
    laster: periodeResultater.some((resultat) => resultat.laster),
    feilmelding: førsteFeil
      ? feilmeldingFra(førsteFeil, "Kunne ikke hente vilkårsvurderinger")
      : undefined,
  };
}
