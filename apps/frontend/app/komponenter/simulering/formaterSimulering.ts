import type { Simuleringsperiode } from "~/api/generated/types.gen";
import { formaterDato } from "~/komponenter/infotrygd/formatering";
import { formaterBelop, formaterIsoDatoTid } from "~/utils/utils";

const MÅNED_OG_ÅR = new Intl.DateTimeFormat("nb-NO", {
  month: "short",
  year: "numeric",
  timeZone: "UTC",
});

const MÅNED_OG_ÅR_LANG = new Intl.DateTimeFormat("nb-NO", {
  month: "long",
  year: "numeric",
  timeZone: "UTC",
});

const ÅR_OG_MÅNED = /^(\d{4})-(\d{2})/;

/** Kort måned og år fra en ISO-dato, for eksempel «feb. 2026». Dagen brukes ikke. */
export function formaterMåned(isoDato: string): string {
  const treff = ÅR_OG_MÅNED.exec(isoDato);
  if (!treff) return isoDato;

  const måned = Number(treff[2]);
  if (måned < 1 || måned > 12) return isoDato;

  return MÅNED_OG_ÅR.format(new Date(Date.UTC(Number(treff[1]), måned - 1, 1)));
}

/** Lang måned og år fra en ISO-dato, for eksempel «desember 2026». Dagen brukes ikke. */
export function formaterMånedLang(isoDato: string): string {
  const treff = ÅR_OG_MÅNED.exec(isoDato);
  if (!treff) return isoDato;

  const måned = Number(treff[2]);
  if (måned < 1 || måned > 12) return isoDato;

  return MÅNED_OG_ÅR_LANG.format(new Date(Date.UTC(Number(treff[1]), måned - 1, 1)));
}

/** Gjør første bokstav stor: «desember 2026» blir «Desember 2026». */
export function medStorForbokstav(tekst: string): string {
  return tekst.charAt(0).toUpperCase() + tekst.slice(1);
}

export function formaterAntallMåneder(antallMåneder: number): string {
  return antallMåneder === 1 ? "1 måned" : `${antallMåneder} måneder`;
}

/** «feb. 2026–nov. 2026», eller bare «des. 2026» når perioden er én måned. */
export function formaterPeriode(fom: string, tom: string, antallMåneder: number): string {
  if (antallMåneder <= 1) return formaterMåned(fom);

  return `${formaterMåned(fom)}–${formaterMåned(tom)}`;
}

/**
 * Tittel over oppsummeringen. «Totalt for februar 2026» når perioden er i én måned, ellers
 * «Totalt for perioden 01.02.2026 – 30.11.2026». «Totalt» når fom eller tom mangler.
 */
export function formaterOppsummeringTittel(
  fom: string | null | undefined,
  tomSisteUtbetaling: string | null | undefined
): string {
  if (!fom || !tomSisteUtbetaling) return "Totalt";

  const erSammeÅrOgMåned = fom.slice(0, 7) === tomSisteUtbetaling.slice(0, 7);
  if (erSammeÅrOgMåned) return `Totalt for ${formaterMånedLang(fom)}`;

  return `Totalt for perioden ${formaterDato(fom)} – ${formaterDato(tomSisteUtbetaling)}`;
}

/** Beløpet med «kr», eller «–» når beløpet er 0. */
export function formaterBeløpEllerStrek(beløp: number): string {
  return beløp === 0 ? "–" : formaterBelop(beløp);
}
export function formaterBeløpPerMåned(beløpPerMåned: number): string {
  return `${formaterBelop(beløpPerMåned)} per måned`;
}

export function formaterSimulertTidspunkt(simulertTidspunkt: string | null | undefined): string {
  return simulertTidspunkt ? formaterIsoDatoTid(simulertTidspunkt) : "–";
}

export function harManuellPostering(perioder: Simuleringsperiode[]): boolean {
  return perioder.some(harManuellPosteringIPerioden);
}

/**
 * Som `erManuellPosteringSamtidigSomResultatIkkeErNull` i ba-sak-frontend: varselet om manuelle posteringer vises
 * bare når en periode har både manuell postering og et resultat som ikke er 0.
 */
export function harManuellPosteringMedResultat(perioder: Simuleringsperiode[]): boolean {
  return perioder.some(
    (periode) => harManuellPosteringIPerioden(periode) && periode.beløpPerMåned.resultat !== 0
  );
}

/** Bare posteringer med en kjent manuell fagområdekode teller. Ukjente koder gir varselet øverst i stedet. */
export function harManuellPosteringIPerioden(periode: Simuleringsperiode): boolean {
  return periode.beløpPerMåned.manuellPostering !== 0;
}

export function erFeilutbetaling(periode: Simuleringsperiode): boolean {
  return periode.beløpPerMåned.feilutbetaling > 0;
}

/**
 * Perioden gir etterbetaling: posteringene har forfalt, og bruker har mer til gode. Backend slår ikke sammen måneder
 * med ulik etterbetaling, så taggen gjelder alle månedene i perioden.
 */
export function erEtterbetaling(periode: Simuleringsperiode): boolean {
  return periode.beløpPerMåned.etterbetaling > 0;
}
