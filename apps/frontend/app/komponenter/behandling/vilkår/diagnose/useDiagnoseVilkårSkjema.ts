import { useState } from "react";
import type { VilkårDiagnoseResponse } from "~/types/vilkår";
import { useVilkårSkjema, type Skjemafeil } from "../felles/useVilkårSkjema";

const DIAGNOSE_FEIL = "Du må oppgi diagnosen, for eksempel diagnosekode eller diagnosenavn.";

export interface DiagnoseGruppe {
  nøkkel: string;
  tittel: string;
}

export function diagnoseNøkkel(diagnose: string): string {
  return diagnose.trim().toLowerCase();
}

export function useDiagnoseVilkårSkjema() {
  const [diagnose, settDiagnose] = useState("");
  const [erYrkesskade, settErYrkesskade] = useState(false);
  const [åpenGruppe, settÅpenGruppe] = useState<string | undefined>(undefined);

  const { åpneNyPeriode: åpneTomtSkjema, ...fellesSkjema } = useVilkårSkjema<"diagnose">(
    (): Skjemafeil<"diagnose"> => (diagnose.trim() === "" ? { diagnose: DIAGNOSE_FEIL } : {})
  );

  const åpneNyDiagnose = () => {
    settDiagnose("");
    settErYrkesskade(false);
    settÅpenGruppe(undefined);
    åpneTomtSkjema();
  };

  const åpneNyPeriodeForDiagnose = (gruppe: DiagnoseGruppe) => {
    settDiagnose(gruppe.tittel);
    settErYrkesskade(false);
    settÅpenGruppe(gruppe.nøkkel);
    åpneTomtSkjema();
  };

  const åpneRedigeringAvPeriode = (periode: VilkårDiagnoseResponse) => {
    settDiagnose(periode.diagnose);
    settErYrkesskade(periode.erYrkesskade);
    settÅpenGruppe(diagnoseNøkkel(periode.diagnose));
    fellesSkjema.åpneRedigeringAvPeriode(periode);
  };

  const lukk = () => {
    settÅpenGruppe(undefined);
    fellesSkjema.lukk();
  };

  return {
    ...fellesSkjema,
    diagnose,
    settDiagnose,
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
