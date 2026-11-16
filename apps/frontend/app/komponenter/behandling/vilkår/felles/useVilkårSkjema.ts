import { useState } from "react";
import type { Vurdering } from "~/types/vilkår";
import {
  usePeriodeMedBegrunnelseSkjema,
  type PeriodeFelt,
  type PeriodeSkjemafeil,
} from "./usePeriodeMedBegrunnelseSkjema";

export {
  FRA_OG_MED_PÅKREVD,
  fraIsoDato,
  tilIsoDato,
  visDato,
  visPeriode,
} from "./usePeriodeMedBegrunnelseSkjema";

const VURDERING_PÅKREVD_FEIL = "Du må velge Ja eller Nei på om vilkåret er oppfylt.";

export interface FellesVilkårPeriodeFelter {
  id: string;
  vurdering: Vurdering;
  begrunnelse: string;
  fraOgMedDato?: string | null;
  tilOgMedDato?: string | null;
}

/** Feltene som det felles skjemaet selv validerer. */
export type FellesFelt = "vurdering" | PeriodeFelt;

/**
 * Feilmeldinger per felt. `EkstraFelt` er feltnavnene det enkelte vilkåret legger til,
 * slik at typos i feltnavn blir kompileringsfeil.
 */
export type Skjemafeil<EkstraFelt extends string = never> = Partial<
  Record<FellesFelt | EkstraFelt, string>
>;

export function useVilkårSkjema<EkstraFelt extends string = never>(
  ekstraValidering?: (vurdering: Vurdering | "") => Skjemafeil<EkstraFelt>,
  innstillinger?: {
    fraOgMedPåkrevd?: (vurdering: Vurdering | "") => boolean;
    /** Feilmelding når vurderingen mangler. Standard er Ja/Nei-teksten. */
    vurderingPåkrevdFeil?: string;
  }
) {
  const [vurdering, settVurdering] = useState<Vurdering | "">("");

  const periodeSkjema = usePeriodeMedBegrunnelseSkjema<EkstraFelt | "vurdering">({
    // Vurdering-feilen legges først, så kallerens ekstraValidering kan overstyre den.
    ekstraValidering: (): PeriodeSkjemafeil<EkstraFelt | "vurdering"> => {
      const feil: PeriodeSkjemafeil<EkstraFelt | "vurdering"> = {};
      if (vurdering === "") {
        feil.vurdering = innstillinger?.vurderingPåkrevdFeil ?? VURDERING_PÅKREVD_FEIL;
      }
      return { ...feil, ...ekstraValidering?.(vurdering) };
    },
    fraOgMedPåkrevd: () => innstillinger?.fraOgMedPåkrevd?.(vurdering) ?? false,
    nullstillEkstra: () => settVurdering(""),
  });

  const åpneRedigeringAvPeriode = (periode: FellesVilkårPeriodeFelter) => {
    settVurdering(periode.vurdering);
    periodeSkjema.åpneRedigeringAvPeriode(periode);
  };

  const fellesVerdier = () => ({
    ...periodeSkjema.periodeVerdier(),
    vurdering: vurdering as Vurdering,
  });

  return {
    ...periodeSkjema,
    vurdering,
    settVurdering,
    åpneRedigeringAvPeriode,
    fellesVerdier,
  };
}

export type VilkårSkjema<EkstraFelt extends string = never> = ReturnType<
  typeof useVilkårSkjema<EkstraFelt>
>;
