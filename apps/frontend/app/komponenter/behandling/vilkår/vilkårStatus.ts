import type {
  VilkårDiagnoseResponse,
  VilkårInstitusjonResponse,
  VilkårMedlemskapResponse,
} from "~/api/generated/types.gen";
import type { VilkårNøkkel } from "~/types/vilkår";
import { feilmeldingFra } from "./felles/feilmeldingFra";
import type { PerioderResult } from "./felles/usePerioder";

export interface VilkårStatus {
  laster: boolean;
  feilmelding?: string;
  antallPerioder: Record<VilkårNøkkel, number>;
  alleVilkårErUtfylt: boolean;
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

  const antallPerioder: Record<VilkårNøkkel, number> = {
    medlemskap: medlemskapsPerioder.data?.length ?? 0,
    diagnose: diagnosePerioder.data?.length ?? 0,
    institusjon: institusjonsPerioder.data?.length ?? 0,
  };

  return {
    laster: periodeResultater.some((resultat) => resultat.laster),
    feilmelding: førsteFeil
      ? feilmeldingFra(førsteFeil, "Kunne ikke hente vilkårsvurderinger")
      : undefined,
    antallPerioder,
    alleVilkårErUtfylt: Object.values(antallPerioder).every((antall) => antall > 0),
  };
}
