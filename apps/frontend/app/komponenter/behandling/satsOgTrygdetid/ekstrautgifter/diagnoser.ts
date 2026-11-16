import {
  formatertKode,
  visningAvDiagnosekode,
} from "~/komponenter/behandling/vilkår/diagnose/diagnosekode";
import { Vurdering, type DiagnosekodeResponse, type VilkårDiagnoseResponse } from "~/types/vilkår";

export function hentUnikeDiagnoserVurdertTilJa(
  perioder: VilkårDiagnoseResponse[] | undefined
): DiagnosekodeResponse[] {
  const jaPerioder = (perioder ?? []).filter((periode) => periode.vurdering === Vurdering.JA);
  const unike = new Map<string, DiagnosekodeResponse>(
    jaPerioder.map((periode) => [periode.diagnose.kode, periode.diagnose] as const)
  );
  return [...unike.values()].toSorted((a, b) => a.kode.localeCompare(b.kode));
}

export function lagDiagnoseVisning(
  perioder: VilkårDiagnoseResponse[] | undefined
): (kode: string) => string {
  const diagnoseEtterKode = new Map(
    (perioder ?? []).map((periode) => [periode.diagnose.kode, periode.diagnose])
  );
  return (kode: string) => {
    const diagnose = diagnoseEtterKode.get(kode);
    return diagnose ? visningAvDiagnosekode(diagnose) : formatertKode(kode);
  };
}
