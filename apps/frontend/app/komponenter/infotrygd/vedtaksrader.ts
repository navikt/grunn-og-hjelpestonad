import type {
  InfotrygdDelytelseResponse,
  InfotrygdEvaluering,
  InfotrygdGhResponse,
  InfotrygdKildevedtak,
  InfotrygdStonadMedArv,
  InfotrygdVedtakMedArv,
} from "~/api/generated/types.gen";

export type Stønad = Omit<InfotrygdStonadMedArv, "vedtak">;

/** Beløpet vises i raden bare når vedtaket har nøyaktig én delytelse. */
export type Radbeløp = { type: "ETT"; beløp: number } | { type: "FLERE" } | { type: "INGEN" };

export type Vedtaksrad = {
  stønad: Stønad;
  vedtak: InfotrygdVedtakMedArv;
  beløp: Radbeløp;
  /** Innvilget tom mangler, og stønaden er ikke opphørt. */
  løpende: boolean;
  /** Opphørsdatoen til stønaden, når innvilget tom mangler og stønaden er opphørt. */
  opphørt: string | null;
  /** Stønaden har ikke oppdrag-id, så den er ikke sendt til oppdrag. */
  ikkeIverksatt: boolean;
  /** Nyeste fom først. */
  delytelser: InfotrygdDelytelseResponse[];
  evaluering: InfotrygdEvaluering | null;
  evalueringFraVedtak: InfotrygdKildevedtak | null;
  /** Trygdetid og sats fra t_gh, nyeste registrering først. */
  gh: InfotrygdGhResponse[];
  ghFraVedtak: InfotrygdKildevedtak | null;
};

const tid = (tidspunkt: string): number => Date.parse(tidspunkt);

const nyesteGhFørst = (gh: InfotrygdGhResponse[]): InfotrygdGhResponse[] =>
  gh.toSorted((a, b) => tid(b.tidspunktReg) - tid(a.tidspunktReg));

const nyesteDelytelseFørst = (delytelser: InfotrygdDelytelseResponse[]) =>
  delytelser.toSorted(
    (a, b) => b.fom.localeCompare(a.fom) || tid(b.tidspunktReg) - tid(a.tidspunktReg)
  );

function radbeløp(delytelser: InfotrygdDelytelseResponse[]): Radbeløp {
  if (delytelser.length === 1) return { type: "ETT", beløp: delytelser[0].belop };
  return delytelser.length === 0 ? { type: "INGEN" } : { type: "FLERE" };
}

function lagRad(stønad: Stønad, vedtak: InfotrygdVedtakMedArv): Vedtaksrad {
  const åpen = vedtak.datoInnvTom == null;
  const opphørsdato = stønad.datoOpphor ?? null;

  return {
    stønad,
    vedtak,
    beløp: radbeløp(vedtak.delytelser),
    løpende: åpen && opphørsdato === null,
    opphørt: åpen ? opphørsdato : null,
    ikkeIverksatt: stønad.oppdragId == null,
    delytelser: nyesteDelytelseFørst(vedtak.delytelser),
    evaluering: vedtak.evaluering?.verdi ?? null,
    evalueringFraVedtak: vedtak.evaluering?.arvetFraVedtak ?? null,
    gh: nyesteGhFørst(vedtak.gh?.verdi ?? []),
    ghFraVedtak: vedtak.gh?.arvetFraVedtak ?? null,
  };
}

const nyesteVedtakFørst = (a: Vedtaksrad, b: Vedtaksrad): number =>
  tid(b.vedtak.tidspunktReg) - tid(a.vedtak.tidspunktReg) || b.vedtak.vedtakId - a.vedtak.vedtakId;

/** Én rad per vedtak på tvers av stønadene, nyeste vedtak først. Ingen vedtak filtreres bort. */
export function lagVedtaksrader(stønader: InfotrygdStonadMedArv[]): Vedtaksrad[] {
  return stønader
    .flatMap(({ vedtak, ...stønad }) => vedtak.map((v) => lagRad(stønad, v)))
    .toSorted(nyesteVedtakFørst);
}
