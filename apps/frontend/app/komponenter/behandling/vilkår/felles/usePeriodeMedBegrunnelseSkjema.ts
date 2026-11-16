import { useRef, useState } from "react";
import { useRangeDatepicker, type RangeValidationT } from "@navikt/ds-react";
import { format, parseISO } from "date-fns";

/** Backend bruker `LocalDate`, altså `yyyy-MM-dd` uten tidssone. */
export function tilIsoDato(dato: Date | undefined): string | null {
  return dato ? format(dato, "yyyy-MM-dd") : null;
}

export function fraIsoDato(dato: string | null | undefined): Date | undefined {
  return dato ? parseISO(dato) : undefined;
}

export function visDato(dato: string | null | undefined): string {
  const parset = fraIsoDato(dato);
  return parset ? format(parset, "dd.MM.yyyy") : "–";
}

export function visPeriode(fraOgMedDato?: string | null, tilOgMedDato?: string | null): string {
  if (!fraOgMedDato && !tilOgMedDato) {
    return "Hele behandlingsperioden";
  }
  const fra = fraOgMedDato ? visDato(fraOgMedDato) : "Fra fødsel";
  const til = tilOgMedDato ? visDato(tilOgMedDato) : "løpende";
  return `${fra} – ${til}`;
}

/** Feltene som skjemaet selv validerer. */
export type PeriodeFelt = "fraOgMed" | "tilOgMed" | "begrunnelse";

/**
 * Feilmeldinger per felt. `EkstraFelt` er feltnavnene kalleren legger til,
 * slik at typos i feltnavn blir kompileringsfeil.
 */
export type PeriodeSkjemafeil<EkstraFelt extends string = never> = Partial<
  Record<PeriodeFelt | EkstraFelt, string>
>;

export interface PeriodeMedBegrunnelseFelter {
  id: string;
  begrunnelse: string;
  fraOgMedDato?: string | null;
  tilOgMedDato?: string | null;
}

const UGYLDIG_DATO = "Skriv datoen som dd.mm.åååå, for eksempel 01.03.2026.";

/** Eksporteres så kallere kan skille denne feilen fra en ugyldig dato. */
export const FRA_OG_MED_PÅKREVD = "Du må fylle inn fra og med-datoen.";

function oppdaterSkjemafeil<Felt extends string>(
  forrige: PeriodeSkjemafeil<Felt>,
  felt: PeriodeFelt | Felt,
  feilmelding?: string
): PeriodeSkjemafeil<Felt> {
  if (forrige[felt] === feilmelding) {
    return forrige;
  }

  if (feilmelding) {
    return { ...forrige, [felt]: feilmelding };
  }

  const neste = { ...forrige };
  delete neste[felt];
  return neste;
}

/**
 * Felles skjemalogikk for en periode med datoer og begrunnelse.
 * Kalleren eier feltene utover det, og sender dem inn via `innstillinger`.
 */
export function usePeriodeMedBegrunnelseSkjema<EkstraFelt extends string = never>(innstillinger?: {
  /** Kallerens egne feil. Slås sammen over datofeilene. */
  ekstraValidering?: () => PeriodeSkjemafeil<EkstraFelt>;
  /** Om fra og med-datoen må fylles inn. */
  fraOgMedPåkrevd?: () => boolean;
  /** Nullstiller kallerens egen state, kalles sammen med den felles. */
  nullstillEkstra?: () => void;
}) {
  const [erÅpent, settErÅpent] = useState(false);
  const [redigererId, settRedigererId] = useState<string | null>(null);
  const [begrunnelse, settBegrunnelse] = useState("");
  const [skjemafeil, settSkjemafeil] = useState<PeriodeSkjemafeil<EkstraFelt>>({});
  const [harForsøktLagre, settHarForsøktLagre] = useState(false);
  const [backendfeil, settBackendfeil] = useState<string | undefined>(undefined);
  const [fokusFeiloppsummering, settFokusFeiloppsummering] = useState(0);

  const datovalidering = useRef<RangeValidationT | undefined>(undefined);

  const { datepickerProps, fromInputProps, toInputProps, selectedRange, setSelected, reset } =
    useRangeDatepicker({
      onValidate: (validering) => {
        datovalidering.current = validering;
      },
    });

  const nullstill = () => {
    innstillinger?.nullstillEkstra?.();
    settBegrunnelse("");
    settSkjemafeil({});
    settHarForsøktLagre(false);
    settBackendfeil(undefined);
    datovalidering.current = undefined;
    reset();
  };

  const åpneNyPeriode = () => {
    settRedigererId(null);
    nullstill();
    settErÅpent(true);
  };

  const åpneRedigeringAvPeriode = (periode: PeriodeMedBegrunnelseFelter) => {
    settRedigererId(periode.id);
    settBegrunnelse(periode.begrunnelse);
    settSkjemafeil({});
    settHarForsøktLagre(false);
    settBackendfeil(undefined);
    datovalidering.current = undefined;
    setSelected({
      from: fraIsoDato(periode.fraOgMedDato),
      to: fraIsoDato(periode.tilOgMedDato),
    });
    settErÅpent(true);
  };

  const lukk = () => {
    settErÅpent(false);
    settRedigererId(null);
    nullstill();
  };

  const validerHeleSkjemaet = (): PeriodeSkjemafeil<EkstraFelt> => {
    const nyeFeil: PeriodeSkjemafeil<EkstraFelt> = {};

    const fra = datovalidering.current?.from;
    const til = datovalidering.current?.to;

    if (fra && !fra.isEmpty && !fra.isValidDate) {
      nyeFeil.fraOgMed = `Fra og med-datoen er ikke gyldig. ${UGYLDIG_DATO}`;
    }

    if (innstillinger?.fraOgMedPåkrevd?.() && !selectedRange?.from && !nyeFeil.fraOgMed) {
      nyeFeil.fraOgMed = FRA_OG_MED_PÅKREVD;
    }

    const førFraOgMed =
      "Til og med-datoen må være samme dag som eller etter fra og med-datoen. Endre en av datoene.";

    if (til) {
      if (til.isBeforeFrom) {
        nyeFeil.tilOgMed = førFraOgMed;
      } else if (!til.isEmpty && !til.isValidDate) {
        nyeFeil.tilOgMed = `Til og med-datoen er ikke gyldig. ${UGYLDIG_DATO}`;
      }
    } else if (selectedRange?.from && selectedRange?.to && selectedRange.to < selectedRange.from) {
      nyeFeil.tilOgMed = førFraOgMed;
    }

    return { ...nyeFeil, ...innstillinger?.ekstraValidering?.() };
  };

  const validerFelt = (felt: PeriodeFelt | EkstraFelt) => {
    if (!harForsøktLagre) {
      return;
    }

    const alleSkjemafeil = validerHeleSkjemaet();
    settSkjemafeil((forrigeSkjemafeil) => {
      const nyFeil = alleSkjemafeil[felt];
      return oppdaterSkjemafeil(forrigeSkjemafeil, felt, nyFeil);
    });
  };

  const fjernFeil = (felt: PeriodeFelt | EkstraFelt) => {
    settSkjemafeil((forrige) => oppdaterSkjemafeil(forrige, felt));
  };

  const validerValg = (felt: PeriodeFelt | EkstraFelt, feilmelding?: string) => {
    if (!harForsøktLagre) {
      return;
    }

    settSkjemafeil((forrige) => oppdaterSkjemafeil(forrige, felt, feilmelding));
  };

  const forsøkLagre = (lagre: () => void) => {
    settBackendfeil(undefined);
    settHarForsøktLagre(true);

    const alleSkjemafeil = validerHeleSkjemaet();
    settSkjemafeil(alleSkjemafeil);

    if (Object.keys(alleSkjemafeil).length > 0) {
      settFokusFeiloppsummering((teller) => teller + 1);
      return;
    }

    lagre();
  };

  const periodeVerdier = () => ({
    id: redigererId,
    begrunnelse: begrunnelse.trim(),
    fraOgMedDato: tilIsoDato(selectedRange?.from),
    tilOgMedDato: tilIsoDato(selectedRange?.to),
  });

  return {
    erÅpent,
    redigererId,
    begrunnelse,
    settBegrunnelse,
    skjemafeil,
    harForsøktLagre,
    settSkjemafeil,
    backendfeil,
    settBackendfeil,
    fokusFeiloppsummering,
    datepickerProps,
    fromInputProps,
    toInputProps,
    åpneNyPeriode,
    åpneRedigeringAvPeriode,
    lukk,
    validerFelt,
    validerValg,
    fjernFeil,
    forsøkLagre,
    periodeVerdier,
  };
}
