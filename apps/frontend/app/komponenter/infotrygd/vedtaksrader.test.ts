import { describe, it } from "node:test";
import assert from "node:assert/strict";
import type {
  InfotrygdDelytelseResponse,
  InfotrygdEvaluering,
  InfotrygdGhResponse,
  InfotrygdStonadMedArv,
  InfotrygdVedtakMedArv,
} from "~/api/generated/types.gen";
import { lagVedtaksrader } from "./vedtaksrader.ts";
import type { Vedtaksrad } from "./vedtaksrader.ts";

const stønad = (overstyr: Partial<InfotrygdStonadMedArv> = {}): InfotrygdStonadMedArv => ({
  stonadId: 1,
  personnr: "00000000000",
  kodeRutine: "TEST",
  datoStart: "2024-01-01",
  oppdragId: 1,
  vedtak: [],
  ...overstyr,
});

const vedtak = (overstyr: Partial<InfotrygdVedtakMedArv> = {}): InfotrygdVedtakMedArv => ({
  vedtakId: 1,
  tknr: "0000",
  saksblokk: "A",
  saksnr: 1,
  typeSak: "TEST",
  kodeResultat: "TEST",
  datoInnvFom: "2024-01-01",
  datoMottattSak: "2024-01-01",
  kodeVedtaksnivaa: "TEST",
  typeBeregning: "TEST",
  tidspunktReg: "2024-01-01T10:00:00",
  brukerid: "TEST",
  endringer: [],
  delytelser: [],
  gh: null,
  ...overstyr,
});

const delytelse = (
  overstyr: Partial<InfotrygdDelytelseResponse> = {}
): InfotrygdDelytelseResponse => ({
  typeDelytelse: "TEST",
  tidspunktReg: "2024-01-01T10:00:00",
  fom: "2024-01-01",
  belop: 1000,
  typeSats: "TEST",
  typeUtbetaling: "TEST",
  brukerid: "TEST",
  ...overstyr,
});

const gh = (overstyr: Partial<InfotrygdGhResponse> = {}): InfotrygdGhResponse => ({
  tidspunktReg: "2024-01-01T10:00:00",
  trygdetid: 40,
  brukerid: "TEST",
  satsGs: 100,
  ...overstyr,
});

const evaluering = (overstyr: Partial<InfotrygdEvaluering> = {}): InfotrygdEvaluering => ({
  månedNotertIInfotrygd: "2024-01",
  motstridendeFelt: [],
  diagnose1: "X00",
  ...overstyr,
});

function finn(rader: Vedtaksrad[], vedtakId: number): Vedtaksrad {
  const rad = rader.find((r) => r.vedtak.vedtakId === vedtakId);
  if (!rad) throw new Error(`Fant ingen rad for vedtak ${vedtakId}`);
  return rad;
}

describe("lagVedtaksrader", () => {
  describe("beløp", () => {
    it("gir ETT med beløpet når vedtaket har én delytelse", () => {
      const rader = lagVedtaksrader([
        stønad({ vedtak: [vedtak({ delytelser: [delytelse({ belop: 1200 })] })] }),
      ]);
      assert.deepEqual(finn(rader, 1).beløp, { type: "ETT", beløp: 1200 });
    });

    it("gir FLERE når vedtaket har flere delytelser", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [vedtak({ delytelser: [delytelse({ belop: 1200 }), delytelse({ belop: 300 })] })],
        }),
      ]);
      assert.deepEqual(finn(rader, 1).beløp, { type: "FLERE" });
    });

    it("gir INGEN når vedtaket ikke har delytelser", () => {
      const rader = lagVedtaksrader([stønad({ vedtak: [vedtak()] })]);
      assert.deepEqual(finn(rader, 1).beløp, { type: "INGEN" });
    });
  });

  describe("status", () => {
    it("er løpende og uten opphørsdato når innvilget tom og opphør mangler", () => {
      const rad = finn(lagVedtaksrader([stønad({ vedtak: [vedtak()] })]), 1);
      assert.equal(rad.løpende, true);
      assert.equal(rad.opphørt, null);
    });

    it("er ikke løpende og får opphørsdato når innvilget tom mangler og stønaden har datoOpphor", () => {
      const rad = finn(
        lagVedtaksrader([stønad({ datoOpphor: "2024-06-30", vedtak: [vedtak()] })]),
        1
      );
      assert.equal(rad.løpende, false);
      assert.equal(rad.opphørt, "2024-06-30");
    });

    it("er ikke løpende og uten opphørsdato når innvilget tom er satt", () => {
      const rad = finn(
        lagVedtaksrader([stønad({ vedtak: [vedtak({ datoInnvTom: "2024-05-31" })] })]),
        1
      );
      assert.equal(rad.løpende, false);
      assert.equal(rad.opphørt, null);
    });

    it("viser ikke opphørsdato når innvilget tom er satt, også når datoOpphor finnes", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({ datoOpphor: "2024-06-30", vedtak: [vedtak({ datoInnvTom: "2024-05-31" })] }),
        ]),
        1
      );
      assert.equal(rad.løpende, false);
      assert.equal(rad.opphørt, null);
    });

    it("er ikke iverksatt når oppdragId mangler", () => {
      const rad = finn(lagVedtaksrader([stønad({ oppdragId: null, vedtak: [vedtak()] })]), 1);
      assert.equal(rad.ikkeIverksatt, true);
    });

    it("er iverksatt når oppdragId er satt", () => {
      const rad = finn(lagVedtaksrader([stønad({ oppdragId: 42, vedtak: [vedtak()] })]), 1);
      assert.equal(rad.ikkeIverksatt, false);
    });
  });

  describe("sortering", () => {
    it("legger nyeste vedtak først på tvers av stønader", () => {
      const rader = lagVedtaksrader([
        stønad({
          stonadId: 1,
          vedtak: [
            vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" }),
            vedtak({ vedtakId: 3, tidspunktReg: "2024-02-01T10:00:00" }),
          ],
        }),
        stønad({
          stonadId: 2,
          vedtak: [vedtak({ vedtakId: 2, tidspunktReg: "2024-03-01T10:00:00" })],
        }),
      ]);
      assert.deepEqual(
        rader.map((r) => r.vedtak.vedtakId),
        [2, 3, 1]
      );
    });

    it("gir høyeste vedtakId først når tidspunktReg er lik", () => {
      const rader = lagVedtaksrader([
        stønad({ vedtak: [vedtak({ vedtakId: 5 }), vedtak({ vedtakId: 7 })] }),
      ]);
      assert.deepEqual(
        rader.map((r) => r.vedtak.vedtakId),
        [7, 5]
      );
    });

    it("sorterer delytelser med nyeste fom først", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({
            vedtak: [
              vedtak({
                delytelser: [
                  delytelse({ fom: "2024-01-01", belop: 100 }),
                  delytelse({ fom: "2024-03-01", belop: 300 }),
                  delytelse({ fom: "2024-02-01", belop: 200 }),
                ],
              }),
            ],
          }),
        ]),
        1
      );
      assert.deepEqual(
        rad.delytelser.map((d) => d.belop),
        [300, 200, 100]
      );
    });

    it("sorterer delytelser med lik fom etter nyeste tidspunktReg", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({
            vedtak: [
              vedtak({
                delytelser: [
                  delytelse({ fom: "2024-01-01", tidspunktReg: "2024-01-10T10:00:00", belop: 100 }),
                  delytelse({ fom: "2024-01-01", tidspunktReg: "2024-02-10T10:00:00", belop: 200 }),
                ],
              }),
            ],
          }),
        ]),
        1
      );
      assert.deepEqual(
        rad.delytelser.map((d) => d.belop),
        [200, 100]
      );
    });

    it("sorterer gh med nyeste tidspunktReg først", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({
            vedtak: [
              vedtak({
                gh: {
                  verdi: [
                    gh({ tidspunktReg: "2023-01-01T10:00:00", trygdetid: 10 }),
                    gh({ tidspunktReg: "2024-01-01T10:00:00", trygdetid: 20 }),
                    gh({ tidspunktReg: "2023-06-01T10:00:00", trygdetid: 15 }),
                  ],
                  arvetFraVedtak: null,
                },
              }),
            ],
          }),
        ]),
        1
      );
      assert.deepEqual(
        rad.gh.map((g) => g.trygdetid),
        [20, 15, 10]
      );
    });
  });

  describe("kilde fra API", () => {
    const kilde = { vedtakId: 1, vedtakstidspunkt: "2024-01-01T10:00:00" };

    it("gir evalueringFraVedtak null når evalueringen er vedtakets egen", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({
            vedtak: [
              vedtak({
                evaluering: { verdi: evaluering({ diagnose1: "X01" }), arvetFraVedtak: null },
              }),
            ],
          }),
        ]),
        1
      );
      assert.deepEqual(rad.evaluering, evaluering({ diagnose1: "X01" }));
      assert.equal(rad.evalueringFraVedtak, null);
    });

    it("gir evalueringFraVedtak lik arvetFraVedtak når evalueringen er arvet", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({
            vedtak: [
              vedtak({
                vedtakId: 2,
                evaluering: { verdi: evaluering({ diagnose1: "X01" }), arvetFraVedtak: kilde },
              }),
            ],
          }),
        ]),
        2
      );
      assert.deepEqual(rad.evaluering, evaluering({ diagnose1: "X01" }));
      assert.deepEqual(rad.evalueringFraVedtak, kilde);
    });

    it("gir gh tom liste og ghFraVedtak null når gh er null", () => {
      const rad = finn(lagVedtaksrader([stønad({ vedtak: [vedtak({ gh: null })] })]), 1);
      assert.deepEqual(rad.gh, []);
      assert.equal(rad.ghFraVedtak, null);
    });

    it("gir ghFraVedtak lik arvetFraVedtak når gh er arvet", () => {
      const rad = finn(
        lagVedtaksrader([
          stønad({
            vedtak: [
              vedtak({
                vedtakId: 2,
                gh: { verdi: [gh({ trygdetid: 20 })], arvetFraVedtak: kilde },
              }),
            ],
          }),
        ]),
        2
      );
      assert.deepEqual(rad.gh, [gh({ trygdetid: 20 })]);
      assert.deepEqual(rad.ghFraVedtak, kilde);
    });
  });

  describe("tomme lister", () => {
    it("gir tom liste når det ikke finnes stønader", () => {
      assert.deepEqual(lagVedtaksrader([]), []);
    });

    it("gir tom liste når stønadene ikke har vedtak", () => {
      assert.deepEqual(lagVedtaksrader([stønad({ vedtak: [] })]), []);
    });
  });
});
