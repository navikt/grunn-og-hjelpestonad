import { useState } from "react";
import type { TrygdetidRequest, TrygdetidResponse } from "~/api/generated/types.gen";
import {
  usePeriodeMedBegrunnelseSkjema,
  type PeriodeSkjemafeil,
} from "~/komponenter/behandling/vilkår/felles/usePeriodeMedBegrunnelseSkjema";

export const FULL_TRYGDETID = 40;

export type TrygdetidKilde = TrygdetidResponse["kilde"];

/** MIGRERING finnes bare på rader som kom fra migrering. Nye rader kan ikke velge den. */
export const kildeTekst: Record<TrygdetidKilde, string> = {
  ALDERSPENSJON: "Alderspensjon",
  UFØRETRYGD: "Uføretrygd",
  INGEN: "Ingen",
  MIGRERING: "Fra migrering",
};

const KILDE_PÅKREVD = "Du må velge kilde for trygdetiden.";
const ANTALL_ÅR_PÅKREVD = "Du må fylle ut trygdetiden.";
const ANTALL_ÅR_UGYLDIG = `Trygdetiden må være et helt antall år fra 0 til ${FULL_TRYGDETID}.`;

type TrygdetidEkstraFelt = "kilde" | "antallÅr";

function erKildeValg(verdi: string): verdi is TrygdetidKilde | "" {
  return verdi === "" || Object.keys(kildeTekst).includes(verdi);
}

function feilKilde(kilde: TrygdetidKilde | ""): string | undefined {
  return kilde === "" ? KILDE_PÅKREVD : undefined;
}

function feilAntallÅr(antallÅr: string): string | undefined {
  const renset = antallÅr.trim();
  if (renset === "") return ANTALL_ÅR_PÅKREVD;
  if (!/^\d+$/.test(renset) || Number(renset) > FULL_TRYGDETID) return ANTALL_ÅR_UGYLDIG;
  return undefined;
}

function validerKildeOgAntallÅr(
  kilde: TrygdetidKilde | "",
  antallÅr: string
): PeriodeSkjemafeil<TrygdetidEkstraFelt> {
  const nyeFeil: PeriodeSkjemafeil<TrygdetidEkstraFelt> = {};

  const kildeFeil = feilKilde(kilde);
  if (kildeFeil) nyeFeil.kilde = kildeFeil;

  const antallÅrFeil = feilAntallÅr(antallÅr);
  if (antallÅrFeil) nyeFeil.antallÅr = antallÅrFeil;

  return nyeFeil;
}

export function useTrygdetidSkjema() {
  const [erMigrertPeriode, settErMigrertPeriode] = useState(false);
  const [kilde, settKilde] = useState<TrygdetidKilde | "">("");
  const [antallÅr, settAntallÅr] = useState("");

  const periodeSkjema = usePeriodeMedBegrunnelseSkjema<TrygdetidEkstraFelt>({
    ekstraValidering: () => validerKildeOgAntallÅr(kilde, antallÅr),
    nullstillEkstra: () => {
      settKilde("");
      settErMigrertPeriode(false);
      settAntallÅr("");
    },
  });

  const åpneRedigeringAvPeriode = (periode: TrygdetidResponse) => {
    settKilde(periode.kilde);
    settErMigrertPeriode(periode.kilde === "MIGRERING");
    settAntallÅr(String(periode.antallÅr));
    periodeSkjema.åpneRedigeringAvPeriode(periode);
  };

  const velgKilde = (verdi: string) => {
    if (!erKildeValg(verdi)) return;

    settKilde(verdi);
    // Kilden «Ingen» gir ingen reduksjon, så trygdetiden settes til 40 år og låses.
    if (verdi === "INGEN") {
      settAntallÅr(String(FULL_TRYGDETID));
      periodeSkjema.fjernFeil("antallÅr");
    }
    periodeSkjema.validerValg("kilde", feilKilde(verdi));
  };

  const endreAntallÅr = (verdi: string) => {
    settAntallÅr(verdi);
    periodeSkjema.validerValg("antallÅr", feilAntallÅr(verdi));
  };

  const verdier = (): TrygdetidRequest => {
    const periode = periodeSkjema.periodeVerdier();
    return {
      id: periode.id,
      antallÅr: kilde === "INGEN" ? FULL_TRYGDETID : Number(antallÅr.trim()),
      // forsøkLagre har validert at kilden er valgt før verdier() kalles.
      kilde: kilde as TrygdetidKilde,
      begrunnelse: periode.begrunnelse,
      fraOgMedDato: periode.fraOgMedDato,
      tilOgMedDato: periode.tilOgMedDato,
    };
  };

  return {
    erÅpent: periodeSkjema.erÅpent,
    redigererId: periodeSkjema.redigererId,
    kilde,
    erMigrertPeriode,
    antallÅr,
    begrunnelse: periodeSkjema.begrunnelse,
    settBegrunnelse: periodeSkjema.settBegrunnelse,
    skjemafeil: periodeSkjema.skjemafeil,
    harForsøktLagre: periodeSkjema.harForsøktLagre,
    backendfeil: periodeSkjema.backendfeil,
    settBackendfeil: periodeSkjema.settBackendfeil,
    fokusFeiloppsummering: periodeSkjema.fokusFeiloppsummering,
    datepickerProps: periodeSkjema.datepickerProps,
    fromInputProps: periodeSkjema.fromInputProps,
    toInputProps: periodeSkjema.toInputProps,
    åpneNyPeriode: periodeSkjema.åpneNyPeriode,
    åpneRedigeringAvPeriode,
    lukk: periodeSkjema.lukk,
    validerFelt: periodeSkjema.validerFelt,
    velgKilde,
    endreAntallÅr,
    forsøkLagre: periodeSkjema.forsøkLagre,
    verdier,
  };
}

export type TrygdetidSkjemaState = ReturnType<typeof useTrygdetidSkjema>;
