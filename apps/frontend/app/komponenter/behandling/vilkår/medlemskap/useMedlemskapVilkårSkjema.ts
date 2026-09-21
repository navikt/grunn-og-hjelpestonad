import { useState } from "react";
import type { Regelverk, VilkårMedlemskapResponse } from "~/types/vilkår";
import { useVilkårSkjema, type Skjemafeil } from "../felles/useVilkårSkjema";

const REGELVERK_FEIL = "Du må velge hvilket regelverk vurderingen bygger på.";
const STANDARD_REGELVERK: Regelverk = "NASJONALE_REGLER";

export function useMedlemskapVilkårSkjema() {
  const [regelverk, settRegelverk] = useState<Regelverk | "">(STANDARD_REGELVERK);
  const fellesSkjema = useVilkårSkjema<"regelverk">((): Skjemafeil<"regelverk"> =>
    regelverk === "" ? { regelverk: REGELVERK_FEIL } : {}
  );

  const åpneNyPeriode = () => {
    settRegelverk(STANDARD_REGELVERK);
    fellesSkjema.åpneNyPeriode();
  };

  const åpneRedigeringAvPeriode = (periode: VilkårMedlemskapResponse) => {
    settRegelverk(periode.regelverk);
    fellesSkjema.åpneRedigeringAvPeriode(periode);
  };

  const endreRegelverk = (nyttRegelverk: Regelverk | "") => {
    settRegelverk(nyttRegelverk);
    fellesSkjema.validerValg("regelverk", nyttRegelverk === "" ? REGELVERK_FEIL : undefined);
  };

  return {
    ...fellesSkjema,
    regelverk,
    åpneNyPeriode,
    åpneRedigeringAvPeriode,
    endreRegelverk,
  };
}
