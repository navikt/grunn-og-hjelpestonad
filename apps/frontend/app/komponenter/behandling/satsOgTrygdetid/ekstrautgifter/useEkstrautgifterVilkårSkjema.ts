import { useState } from "react";
import {
  hjemmelbokstaverForNyeTilfeller,
  Vurdering,
  type DiagnosekodeResponse,
  type Ekstrautgift,
  type Hjemmelbokstav,
  type Sats,
  type VilkårEkstrautgifterResponse,
} from "~/types/vilkår";
import {
  useVilkårSkjema,
  FRA_OG_MED_PÅKREVD,
  type Skjemafeil,
} from "~/komponenter/behandling/vilkår/felles/useVilkårSkjema";

const SATS_FEIL = "Du må velge sats når vilkåret er oppfylt.";
const EKSTRAUTGIFT_MANGLER_FEIL = "Du må legge til minst én ekstrautgift når vilkåret er oppfylt.";
const HJEMMELBOKSTAV_MANGLER_FEIL = "Du må velge hjemmelbokstav for hver ekstrautgift.";
const DIAGNOSE_MANGLER_FEIL = "Du må velge minst én diagnose for hver ekstrautgift.";

type EkstraFelt = "sats" | "ekstrautgifter";

const VURDERING_PÅKREVD_FEIL = "Du må velge sats eller «Ekstrautgifter under sats 1».";

export const UNDER_SATS_1 = "under-sats-1";

export type SatsValg = Sats | typeof UNDER_SATS_1 | "";

function satsValgFra(vurdering: Vurdering | "", sats: Sats | ""): SatsValg {
  if (vurdering === Vurdering.NEI) return UNDER_SATS_1;
  if (vurdering === Vurdering.JA) return sats;
  return "";
}

export interface EkstrautgiftRad {
  hjemmelbokstav: Hjemmelbokstav | "";
  diagnosekoder: string[];
}

function ekstrautgifterFeil(
  vurdering: Vurdering | "",
  rader: EkstrautgiftRad[]
): string | undefined {
  if (vurdering === Vurdering.JA && rader.length === 0) return EKSTRAUTGIFT_MANGLER_FEIL;
  if (rader.some((rad) => rad.hjemmelbokstav === "")) return HJEMMELBOKSTAV_MANGLER_FEIL;
  if (rader.some((rad) => rad.diagnosekoder.length === 0)) return DIAGNOSE_MANGLER_FEIL;
  return undefined;
}

function validerEkstrautgifter(
  vurdering: Vurdering | "",
  sats: Sats | "",
  rader: EkstrautgiftRad[]
): Skjemafeil<EkstraFelt> {
  const feil: Skjemafeil<EkstraFelt> = {};
  if (vurdering === Vurdering.JA && sats === "") feil.sats = SATS_FEIL;

  const listefeil = ekstrautgifterFeil(vurdering, rader);
  if (listefeil) feil.ekstrautgifter = listefeil;

  return feil;
}

export function useEkstrautgifterVilkårSkjema(diagnoserVurdertTilJa: DiagnosekodeResponse[]) {
  const [sats, settSats] = useState<Sats | "">("");
  const [ekstrautgifter, settEkstrautgifter] = useState<EkstrautgiftRad[]>([]);
  // Bokstavene perioden hadde da redigeringen startet. Bare disse kan velges av D og E.
  const [lagredeBokstaver, settLagredeBokstaver] = useState<Hjemmelbokstav[]>([]);

  const fellesSkjema = useVilkårSkjema<EkstraFelt>(
    (vurdering): Skjemafeil<EkstraFelt> => validerEkstrautgifter(vurdering, sats, ekstrautgifter),
    {
      fraOgMedPåkrevd: (vurdering) => vurdering === Vurdering.JA,
      vurderingPåkrevdFeil: VURDERING_PÅKREVD_FEIL,
    }
  );

  const nyTomEkstrautgift = (): EkstrautgiftRad => ({
    hjemmelbokstav: "",
    diagnosekoder:
      diagnoserVurdertTilJa.length === 1 ? diagnoserVurdertTilJa.map((d) => d.kode) : [],
  });

  const åpneNyPeriode = () => {
    settSats("");
    settLagredeBokstaver([]);
    settEkstrautgifter(diagnoserVurdertTilJa.length > 0 ? [nyTomEkstrautgift()] : []);
    fellesSkjema.åpneNyPeriode();
  };

  const åpneRedigeringAvPeriode = (periode: VilkårEkstrautgifterResponse) => {
    settSats(periode.sats ?? "");
    settEkstrautgifter(
      periode.ekstrautgifter.map((ekstrautgift) => ({
        hjemmelbokstav: ekstrautgift.hjemmelbokstav,
        diagnosekoder: [...ekstrautgift.diagnosekoder],
      }))
    );
    settLagredeBokstaver(periode.ekstrautgifter.map((ekstrautgift) => ekstrautgift.hjemmelbokstav));
    fellesSkjema.åpneRedigeringAvPeriode(periode);
  };

  const oppdaterRader = (neste: EkstrautgiftRad[]) => {
    settEkstrautgifter(neste);
    fellesSkjema.validerValg("ekstrautgifter", ekstrautgifterFeil(fellesSkjema.vurdering, neste));
  };

  const fjernFraOgMedPåkrevdFeil = () => {
    if (fellesSkjema.feil.fraOgMed === FRA_OG_MED_PÅKREVD) fellesSkjema.fjernFeil("fraOgMed");
  };

  const raderEtterVurdering = (vurdering: Vurdering): EkstrautgiftRad[] => {
    if (vurdering === Vurdering.JA) {
      if (ekstrautgifter.length === 0 && diagnoserVurdertTilJa.length > 0) {
        return [nyTomEkstrautgift()];
      }
      return ekstrautgifter;
    }
    return ekstrautgifter.filter((rad) => rad.hjemmelbokstav !== "");
  };

  const vurderingEndret = (vurdering: Vurdering) => {
    if (vurdering !== Vurdering.JA) {
      fellesSkjema.fjernFeil("sats");
      fjernFraOgMedPåkrevdFeil();
    }
    const neste = raderEtterVurdering(vurdering);
    settEkstrautgifter(neste);
    fellesSkjema.validerValg("ekstrautgifter", ekstrautgifterFeil(vurdering, neste));
  };

  const velgSatsValg = (valg: SatsValg) => {
    if (valg === "") {
      settSats("");
      fellesSkjema.settVurdering("");
      fellesSkjema.fjernFeil("sats");
      fjernFraOgMedPåkrevdFeil();
      fellesSkjema.validerValg("ekstrautgifter", ekstrautgifterFeil("", ekstrautgifter));
      fellesSkjema.validerValg("vurdering", VURDERING_PÅKREVD_FEIL);
      return;
    }

    if (valg === UNDER_SATS_1) {
      settSats("");
      fellesSkjema.settVurdering(Vurdering.NEI);
      fellesSkjema.fjernFeil("vurdering");
      vurderingEndret(Vurdering.NEI);
      return;
    }

    settSats(valg);
    fellesSkjema.settVurdering(Vurdering.JA);
    fellesSkjema.fjernFeil("vurdering");
    fellesSkjema.fjernFeil("sats");
    vurderingEndret(Vurdering.JA);
  };

  const leggTilEkstrautgift = () => oppdaterRader([...ekstrautgifter, nyTomEkstrautgift()]);

  const fjernEkstrautgift = (indeks: number) =>
    oppdaterRader(ekstrautgifter.filter((_, i) => i !== indeks));

  const endreHjemmelbokstav = (indeks: number, bokstav: Hjemmelbokstav | "") =>
    oppdaterRader(
      ekstrautgifter.map((rad, i) => (i === indeks ? { ...rad, hjemmelbokstav: bokstav } : rad))
    );

  const endreDiagnoser = (indeks: number, koder: string[]) =>
    oppdaterRader(
      ekstrautgifter.map((rad, i) => (i === indeks ? { ...rad, diagnosekoder: koder } : rad))
    );

  const alleBokstaver = (): Hjemmelbokstav[] => [
    ...new Set([...hjemmelbokstaverForNyeTilfeller, ...lagredeBokstaver]),
  ];

  const valgbareHjemmelbokstaver = (indeks: number): Hjemmelbokstav[] => {
    const valgtAndreSteder = ekstrautgifter
      .filter((_, i) => i !== indeks)
      .map((rad) => rad.hjemmelbokstav);
    return alleBokstaver()
      .filter((bokstav) => !valgtAndreSteder.includes(bokstav))
      .toSorted();
  };

  const kanLeggeTilEkstrautgift = alleBokstaver().some(
    (bokstav) => !ekstrautgifter.some((rad) => rad.hjemmelbokstav === bokstav)
  );

  const verdier = (): { sats: Sats | null; ekstrautgifter: Ekstrautgift[] } => ({
    sats: fellesSkjema.vurdering === Vurdering.JA && sats !== "" ? sats : null,
    ekstrautgifter: ekstrautgifter.map((rad) => ({
      hjemmelbokstav: rad.hjemmelbokstav as Hjemmelbokstav,
      diagnosekoder: rad.diagnosekoder,
    })),
  });

  return {
    ...fellesSkjema,
    sats,
    satsValg: satsValgFra(fellesSkjema.vurdering, sats),
    ekstrautgifter,
    åpneNyPeriode,
    åpneRedigeringAvPeriode,
    velgSatsValg,
    leggTilEkstrautgift,
    fjernEkstrautgift,
    endreHjemmelbokstav,
    endreDiagnoser,
    valgbareHjemmelbokstaver,
    kanLeggeTilEkstrautgift,
    verdier,
  };
}

export type EkstrautgifterSkjema = ReturnType<typeof useEkstrautgifterVilkårSkjema>;
