import { useState } from "react";
import type { TrygdetidRequest, TrygdetidResponse } from "~/api/generated/types.gen";
import {
  usePeriodeMedBegrunnelseSkjema,
  type PeriodeSkjemafeil,
} from "~/komponenter/behandling/vilkår/felles/usePeriodeMedBegrunnelseSkjema";

export const MAKS_TELLER = 40;

export type TrygdetidKilde = TrygdetidResponse["kilde"];

/** MIGRERING finnes bare på rader som kom fra migrering. Nye rader kan ikke velge den. */
export const kildeTekst: Record<TrygdetidKilde, string> = {
  ALDERSPENSJON: "Alderspensjon",
  UFØRETRYGD: "Uføretrygd",
  INGEN: "Ingen",
  MIGRERING: "Fra migrering",
};

const KILDE_PÅKREVD = "Du må velge kilde for trygdetiden.";
const TELLER_PÅKREVD = "Du må fylle ut teller.";
const TELLER_UGYLDIG = `Teller må være et heltall fra 0 til ${MAKS_TELLER}.`;

type TrygdetidEkstraFelt = "kilde" | "teller";

function erKildeValg(verdi: string): verdi is TrygdetidKilde | "" {
  return verdi === "" || Object.keys(kildeTekst).includes(verdi);
}

function feilKilde(kilde: TrygdetidKilde | ""): string | undefined {
  return kilde === "" ? KILDE_PÅKREVD : undefined;
}

function feilTeller(teller: string): string | undefined {
  const renset = teller.trim();
  if (renset === "") return TELLER_PÅKREVD;
  if (!/^\d+$/.test(renset) || Number(renset) > MAKS_TELLER) return TELLER_UGYLDIG;
  return undefined;
}

function validerKildeOgTeller(
  kilde: TrygdetidKilde | "",
  teller: string
): PeriodeSkjemafeil<TrygdetidEkstraFelt> {
  const nyeFeil: PeriodeSkjemafeil<TrygdetidEkstraFelt> = {};

  const kildeFeil = feilKilde(kilde);
  if (kildeFeil) nyeFeil.kilde = kildeFeil;

  const tellerFeil = feilTeller(teller);
  if (tellerFeil) nyeFeil.teller = tellerFeil;

  return nyeFeil;
}

export function useTrygdetidSkjema() {
  const [erMigrertPeriode, settErMigrertPeriode] = useState(false);
  const [kilde, settKilde] = useState<TrygdetidKilde | "">("");
  const [teller, settTeller] = useState("");

  const periodeSkjema = usePeriodeMedBegrunnelseSkjema<TrygdetidEkstraFelt>({
    ekstraValidering: () => validerKildeOgTeller(kilde, teller),
    nullstillEkstra: () => {
      settKilde("");
      settErMigrertPeriode(false);
      settTeller("");
    },
  });

  const åpneRedigeringAvPeriode = (periode: TrygdetidResponse) => {
    settKilde(periode.kilde);
    settErMigrertPeriode(periode.kilde === "MIGRERING");
    settTeller(String(periode.teller));
    periodeSkjema.åpneRedigeringAvPeriode(periode);
  };

  const velgKilde = (verdi: string) => {
    if (!erKildeValg(verdi)) return;

    settKilde(verdi);
    // Kilden «Ingen» gir alltid 40, så telleren settes og låses.
    if (verdi === "INGEN") {
      settTeller(String(MAKS_TELLER));
      periodeSkjema.fjernFeil("teller");
    }
    periodeSkjema.validerValg("kilde", feilKilde(verdi));
  };

  const endreTeller = (verdi: string) => {
    settTeller(verdi);
    periodeSkjema.validerValg("teller", feilTeller(verdi));
  };

  const verdier = (): TrygdetidRequest => {
    const periode = periodeSkjema.periodeVerdier();
    return {
      id: periode.id,
      teller: kilde === "INGEN" ? MAKS_TELLER : Number(teller.trim()),
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
    teller,
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
    endreTeller,
    forsøkLagre: periodeSkjema.forsøkLagre,
    verdier,
  };
}

export type TrygdetidSkjemaState = ReturnType<typeof useTrygdetidSkjema>;
