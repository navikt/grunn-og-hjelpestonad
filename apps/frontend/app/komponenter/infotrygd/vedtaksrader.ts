import type {
  GjeldendeInfotrygdPeriode,
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

export type Gjeldende = "HELT_GJELDENDE" | "DELVIS_GJELDENDE" | "IKKE_GJELDENDE";

export type Vedtaksrad = {
  stønad: Stønad;
  vedtak: InfotrygdVedtakMedArv;
  /** Om vedtaket fremdeles er gjeldende. */
  gjeldende: Gjeldende;
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

export const nyesteGhFørst = (gh: InfotrygdGhResponse[]): InfotrygdGhResponse[] =>
  gh.toSorted((a, b) => tid(b.tidspunktReg) - tid(a.tidspunktReg));

const nyesteDelytelseFørst = (delytelser: InfotrygdDelytelseResponse[]) =>
  delytelser.toSorted(
    (a, b) => b.fom.localeCompare(a.fom) || tid(b.tidspunktReg) - tid(a.tidspunktReg)
  );

function radbeløp(delytelser: InfotrygdDelytelseResponse[]): Radbeløp {
  if (delytelser.length === 1) return { type: "ETT", beløp: delytelser[0].belop };
  return delytelser.length === 0 ? { type: "INGEN" } : { type: "FLERE" };
}

// Backend slår sammen like nabo-perioder, så et vedtak som gjelder i hele
// perioden kommer alltid som én periode.
function gjeldendeStatus(
  vedtak: InfotrygdVedtakMedArv,
  perioder: readonly GjeldendeInfotrygdPeriode[]
): Gjeldende {
  if (perioder.length === 0) return "IKKE_GJELDENDE";
  const hele = perioder.filter(
    (p) => p.fom === vedtak.datoInnvFom && (p.tom ?? null) === (vedtak.datoInnvTom ?? null)
  );
  return hele.length === 1 ? "HELT_GJELDENDE" : "DELVIS_GJELDENDE";
}

function lagRad(
  stønad: Stønad,
  vedtak: InfotrygdVedtakMedArv,
  perioderPerVedtak: ReadonlyMap<number, readonly GjeldendeInfotrygdPeriode[]>
): Vedtaksrad {
  const åpen = vedtak.datoInnvTom == null;
  const opphørsdato = stønad.datoOpphor ?? null;

  return {
    stønad,
    vedtak,
    gjeldende: gjeldendeStatus(vedtak, perioderPerVedtak.get(vedtak.vedtakId) ?? []),
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
export function lagVedtaksrader(
  stønader: InfotrygdStonadMedArv[],
  gjeldendePerioder: readonly GjeldendeInfotrygdPeriode[] = []
): Vedtaksrad[] {
  const perioderPerVedtak = Map.groupBy(gjeldendePerioder, (periode) => periode.vedtak.vedtakId);
  return stønader
    .flatMap(({ vedtak, ...stønad }) => vedtak.map((v) => lagRad(stønad, v, perioderPerVedtak)))
    .toSorted(nyesteVedtakFørst);
}
