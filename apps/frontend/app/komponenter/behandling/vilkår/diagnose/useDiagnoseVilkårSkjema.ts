import { useState } from "react";
import type { DiagnosekodeResponse, VilkårDiagnoseResponse } from "~/types/vilkår";
import { useVilkårSkjema, type Skjemafeil } from "../felles/useVilkårSkjema";
import type { VilkårPeriodeGruppe } from "../felles/VilkårSeksjon";

const DIAGNOSE_FEIL = "Du må velge en diagnose fra kodeverket.";

export function useDiagnoseVilkårSkjema() {
  const [diagnosekode, settDiagnosekode] = useState<DiagnosekodeResponse | undefined>(undefined);
  const [erYrkesskade, settErYrkesskade] = useState(false);
  const [åpenGruppe, settÅpenGruppe] = useState<string | undefined>(undefined);

  const { åpneNyPeriode: åpneTomtSkjema, ...fellesSkjema } = useVilkårSkjema<"diagnose">(
    (): Skjemafeil<"diagnose"> => (diagnosekode === undefined ? { diagnose: DIAGNOSE_FEIL } : {})
  );

  const velgDiagnose = (valgt: DiagnosekodeResponse | undefined) => {
    settDiagnosekode(valgt);
    if (valgt) fellesSkjema.fjernFeil("diagnose");
  };

  const åpneNyDiagnose = () => {
    settDiagnosekode(undefined);
    settErYrkesskade(false);
    settÅpenGruppe(undefined);
    åpneTomtSkjema();
  };

  const åpneNyPeriodeForDiagnose = (gruppe: VilkårPeriodeGruppe<VilkårDiagnoseResponse>) => {
    const [periode] = gruppe.perioder;
    settDiagnosekode(periode?.diagnose);
    settErYrkesskade(false);
    settÅpenGruppe(gruppe.nøkkel);
    åpneTomtSkjema();
  };

  const åpneRedigeringAvPeriode = (periode: VilkårDiagnoseResponse) => {
    settDiagnosekode(periode.diagnose);
    settErYrkesskade(periode.erYrkesskade);
    settÅpenGruppe(periode.diagnose.kode);
    fellesSkjema.åpneRedigeringAvPeriode(periode);
  };

  const lukk = () => {
    settÅpenGruppe(undefined);
    fellesSkjema.lukk();
  };

  return {
    ...fellesSkjema,
    diagnosekode,
    velgDiagnose,
    erYrkesskade,
    settErYrkesskade,
    åpenGruppe,
    diagnoseErLåst: åpenGruppe !== undefined && fellesSkjema.redigererId === null,
    åpneNyDiagnose,
    åpneNyPeriodeForDiagnose,
    åpneRedigeringAvPeriode,
    lukk,
  };
}
