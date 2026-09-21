import { useRef, useState } from "react";
import { useRangeDatepicker, type RangeValidationT } from "@navikt/ds-react";
import { format, parseISO } from "date-fns";
import type { Vurdering } from "~/types/vilkår";

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

export interface FellesVilkårPeriodeFelter {
  id: string;
  vurdering: Vurdering;
  begrunnelse: string;
  fraOgMedDato?: string | null;
  tilOgMedDato?: string | null;
}

/** Feltene som det felles skjemaet selv validerer. */
export type FellesFelt = "vurdering" | "fraOgMed" | "tilOgMed" | "begrunnelse";

/**
 * Feilmeldinger per felt. `EkstraFelt` er feltnavnene det enkelte vilkåret legger til,
 * slik at typos i feltnavn blir kompileringsfeil.
 */
export type Skjemafeil<EkstraFelt extends string = never> = Partial<
  Record<FellesFelt | EkstraFelt, string>
>;

const UGYLDIG_DATO = "Skriv datoen som dd.mm.åååå, for eksempel 01.03.2026.";

function oppdaterSkjemafeil<Felt extends string>(
  forrige: Skjemafeil<Felt>,
  felt: FellesFelt | Felt,
  feilmelding?: string
): Skjemafeil<Felt> {
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

export function useVilkårSkjema<EkstraFelt extends string = never>(
  ekstraValidering?: () => Skjemafeil<EkstraFelt>
) {
  const [erÅpent, settErÅpent] = useState(false);
  const [redigererId, settRedigererId] = useState<string | null>(null);
  const [vurdering, settVurdering] = useState<Vurdering | "">("");
  const [begrunnelse, settBegrunnelse] = useState("");
  const [feil, settFeil] = useState<Skjemafeil<EkstraFelt>>({});
  const [harForsøktLagre, settHarForsøktLagre] = useState(false);
  const [lagringsfeil, settLagringsfeil] = useState<string | undefined>(undefined);
  const [fokusFeiloppsummering, settFokusFeiloppsummering] = useState(0);

  const datovalidering = useRef<RangeValidationT | undefined>(undefined);

  const { datepickerProps, fromInputProps, toInputProps, selectedRange, setSelected, reset } =
    useRangeDatepicker({
      onValidate: (validering) => {
        datovalidering.current = validering;
      },
    });

  const nullstill = () => {
    settVurdering("");
    settBegrunnelse("");
    settFeil({});
    settHarForsøktLagre(false);
    settLagringsfeil(undefined);
    datovalidering.current = undefined;
    reset();
  };

  const åpneNyPeriode = () => {
    settRedigererId(null);
    nullstill();
    settErÅpent(true);
  };

  const åpneRedigeringAvPeriode = (periode: FellesVilkårPeriodeFelter) => {
    settRedigererId(periode.id);
    settVurdering(periode.vurdering);
    settBegrunnelse(periode.begrunnelse);
    settFeil({});
    settHarForsøktLagre(false);
    settLagringsfeil(undefined);
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

  const validerHeleSkjemaet = (): Skjemafeil<EkstraFelt> => {
    const nyeFeil: Skjemafeil<EkstraFelt> = {};

    if (vurdering === "") {
      nyeFeil.vurdering = "Du må velge Ja eller Nei på om vilkåret er oppfylt.";
    }

    const fra = datovalidering.current?.from;
    const til = datovalidering.current?.to;

    if (fra && !fra.isEmpty && !fra.isValidDate) {
      nyeFeil.fraOgMed = `Fra og med-datoen er ikke gyldig. ${UGYLDIG_DATO}`;
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

    return { ...nyeFeil, ...ekstraValidering?.() };
  };

  const validerFelt = (felt: FellesFelt | EkstraFelt) => {
    if (!harForsøktLagre) {
      return;
    }

    const alleSkjemafeil = validerHeleSkjemaet();
    settFeil((forrigeSkjemafeil) => {
      const nyFeil = alleSkjemafeil[felt];
      return oppdaterSkjemafeil(forrigeSkjemafeil, felt, nyFeil);
    });
  };

  const fjernFeil = (felt: FellesFelt | EkstraFelt) => {
    settFeil((forrige) => oppdaterSkjemafeil(forrige, felt));
  };

  const validerValg = (felt: FellesFelt | EkstraFelt, feilmelding?: string) => {
    if (!harForsøktLagre) {
      return;
    }

    settFeil((forrige) => oppdaterSkjemafeil(forrige, felt, feilmelding));
  };

  const forsøkLagre = (lagre: () => void) => {
    settLagringsfeil(undefined);
    settHarForsøktLagre(true);

    const alleSkjemafeil = validerHeleSkjemaet();
    settFeil(alleSkjemafeil);

    if (Object.keys(alleSkjemafeil).length > 0) {
      settFokusFeiloppsummering((teller) => teller + 1);
      return;
    }

    lagre();
  };

  const fellesVerdier = () => ({
    id: redigererId,
    vurdering: vurdering as Vurdering,
    begrunnelse: begrunnelse.trim(),
    fraOgMedDato: tilIsoDato(selectedRange?.from),
    tilOgMedDato: tilIsoDato(selectedRange?.to),
  });

  return {
    erÅpent,
    redigererId,
    vurdering,
    settVurdering,
    begrunnelse,
    settBegrunnelse,
    feil,
    harForsøktLagre,
    settFeil,
    lagringsfeil,
    settLagringsfeil,
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
    fellesVerdier,
  };
}

export type VilkårSkjema<EkstraFelt extends string = never> = ReturnType<
  typeof useVilkårSkjema<EkstraFelt>
>;
