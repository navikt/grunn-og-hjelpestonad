import type {
  InfotrygdDelytelseResponse,
  InfotrygdEvaluering,
  InfotrygdGhResponse,
  InfotrygdStonadResponse,
  InfotrygdVedtakResponse,
} from "~/api/generated/types.gen";

export type Kildevedtak = { vedtakId: number; vedtakstidspunkt: string };

export type Stønad = Omit<InfotrygdStonadResponse, "vedtak">;

/** Beløpet vises i raden bare når vedtaket har nøyaktig én delytelse. */
export type Radbeløp = { type: "ETT"; beløp: number } | { type: "FLERE" } | { type: "INGEN" };

export type Vedtaksrad = {
  stønad: Stønad;
  vedtak: InfotrygdVedtakResponse;
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
  evalueringFraVedtak: Kildevedtak | null;
  /** Trygdetid og sats fra t_gh, nyeste registrering først. */
  gh: InfotrygdGhResponse[];
  ghFraVedtak: Kildevedtak | null;
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

function lagRad(stønad: Stønad, vedtak: InfotrygdVedtakResponse): Vedtaksrad {
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
    evaluering: vedtak.evaluering ?? null,
    evalueringFraVedtak: null,
    gh: nyesteGhFørst(vedtak.gh),
    ghFraVedtak: null,
  };
}

const kildevedtak = (rad: Vedtaksrad): Kildevedtak => ({
  vedtakId: rad.vedtak.vedtakId,
  vedtakstidspunkt: rad.vedtak.tidspunktReg,
});

// Egen verdi er en verdi som ikke er arvet fra et annet vedtak.
const harEgenEvaluering = (rad: Vedtaksrad): boolean =>
  rad.evalueringFraVedtak === null && rad.evaluering !== null;

const harEgenGh = (rad: Vedtaksrad): boolean => rad.ghFraVedtak === null && rad.gh.length > 0;

// Nærmeste tidligere vedtak på samme stønad som har en egen verdi. Samme vedtakstidspunkt regnes ikke som tidligere.
function finnSisteTidligereVerdi(
  rader: Vedtaksrad[],
  rad: Vedtaksrad,
  harVerdi: (kandidat: Vedtaksrad) => boolean
): Vedtaksrad | undefined {
  const tidspunkt = tid(rad.vedtak.tidspunktReg);

  return rader
    .filter(
      (kandidat) =>
        kandidat.stønad.stonadId === rad.stønad.stonadId &&
        kandidat.vedtak.vedtakId !== rad.vedtak.vedtakId &&
        tid(kandidat.vedtak.tidspunktReg) < tidspunkt &&
        harVerdi(kandidat)
    )
    .reduce<Vedtaksrad | undefined>(
      (nærmest, kandidat) =>
        nærmest === undefined ||
        tid(kandidat.vedtak.tidspunktReg) > tid(nærmest.vedtak.tidspunktReg)
          ? kandidat
          : nærmest,
      undefined
    );
}

// Maskinvedtak mangler ofte evaluering (EVGH) og trygdetid og sats (t_gh).
// Da vises verdien fra nærmeste tidligere vedtak på samme stønad, merket med kilden.
export function medDataFraTidligereVedtak(rader: Vedtaksrad[]): Vedtaksrad[] {
  return rader.map((rad) => {
    const evalueringKilde =
      rad.evaluering === null ? finnSisteTidligereVerdi(rader, rad, harEgenEvaluering) : undefined;
    const ghKilde =
      rad.gh.length === 0 ? finnSisteTidligereVerdi(rader, rad, harEgenGh) : undefined;

    return {
      ...rad,
      evaluering: evalueringKilde ? evalueringKilde.evaluering : rad.evaluering,
      evalueringFraVedtak: evalueringKilde ? kildevedtak(evalueringKilde) : rad.evalueringFraVedtak,
      gh: ghKilde ? ghKilde.gh : rad.gh,
      ghFraVedtak: ghKilde ? kildevedtak(ghKilde) : rad.ghFraVedtak,
    };
  });
}

const nyesteVedtakFørst = (a: Vedtaksrad, b: Vedtaksrad): number =>
  tid(b.vedtak.tidspunktReg) - tid(a.vedtak.tidspunktReg) || b.vedtak.vedtakId - a.vedtak.vedtakId;

/** Én rad per vedtak på tvers av stønadene, nyeste vedtak først. Ingen vedtak filtreres bort. */
export function lagVedtaksrader(stønader: InfotrygdStonadResponse[]): Vedtaksrad[] {
  const rader = stønader.flatMap(({ vedtak, ...stønad }) => vedtak.map((v) => lagRad(stønad, v)));
  return medDataFraTidligereVedtak(rader).toSorted(nyesteVedtakFørst);
}
