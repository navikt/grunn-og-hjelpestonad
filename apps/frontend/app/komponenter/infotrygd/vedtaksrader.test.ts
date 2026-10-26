import { describe, it } from "node:test";
import assert from "node:assert/strict";
import type {
  InfotrygdDelytelseResponse,
  InfotrygdEvaluering,
  InfotrygdGhResponse,
  InfotrygdStonadResponse,
  InfotrygdVedtakResponse,
} from "~/api/generated/types.gen";
import { lagVedtaksrader, medDataFraTidligereVedtak } from "./vedtaksrader.ts";
import type { Vedtaksrad } from "./vedtaksrader.ts";

const stønad = (overstyr: Partial<InfotrygdStonadResponse> = {}): InfotrygdStonadResponse => ({
  stonadId: 1,
  personnr: "00000000000",
  kodeRutine: "TEST",
  datoStart: "2024-01-01",
  oppdragId: 1,
  vedtak: [],
  ...overstyr,
});

const vedtak = (overstyr: Partial<InfotrygdVedtakResponse> = {}): InfotrygdVedtakResponse => ({
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
  gh: [],
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

// Rad for ett vedtak alene på stønaden, så lagVedtaksrader gir ingen arv.
function enRad(v: InfotrygdVedtakResponse): Vedtaksrad {
  return finn(lagVedtaksrader([stønad({ vedtak: [v] })]), v.vedtakId);
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
                gh: [
                  gh({ tidspunktReg: "2023-01-01T10:00:00", trygdetid: 10 }),
                  gh({ tidspunktReg: "2024-01-01T10:00:00", trygdetid: 20 }),
                  gh({ tidspunktReg: "2023-06-01T10:00:00", trygdetid: 15 }),
                ],
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

  describe("arv av evaluering", () => {
    it("arver fra nærmeste tidligere vedtak med egen evaluering", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              evaluering: evaluering({ diagnose1: "X01" }),
            }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-02-01T10:00:00",
              evaluering: evaluering({ diagnose1: "X02" }),
            }),
            vedtak({ vedtakId: 3, tidspunktReg: "2024-03-01T10:00:00" }),
          ],
        }),
      ]);
      const rad = finn(rader, 3);
      assert.deepEqual(rad.evaluering, evaluering({ diagnose1: "X02" }));
      assert.deepEqual(rad.evalueringFraVedtak, {
        vedtakId: 2,
        vedtakstidspunkt: "2024-02-01T10:00:00",
      });
    });

    it("arver ikke fra senere vedtak", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-02-01T10:00:00",
              evaluering: evaluering(),
            }),
          ],
        }),
      ]);
      const rad = finn(rader, 1);
      assert.equal(rad.evaluering, null);
      assert.equal(rad.evalueringFraVedtak, null);
    });

    it("arver ikke fra vedtak på en annen stønad", () => {
      const rader = lagVedtaksrader([
        stønad({
          stonadId: 1,
          vedtak: [vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" })],
        }),
        stønad({
          stonadId: 2,
          vedtak: [
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2023-12-01T10:00:00",
              evaluering: evaluering(),
            }),
          ],
        }),
      ]);
      const rad = finn(rader, 1);
      assert.equal(rad.evaluering, null);
      assert.equal(rad.evalueringFraVedtak, null);
    });

    it("arver ikke fra vedtak med samme tidspunktReg", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-01-01T10:00:00",
              evaluering: evaluering(),
            }),
          ],
        }),
      ]);
      const rad = finn(rader, 1);
      assert.equal(rad.evaluering, null);
      assert.equal(rad.evalueringFraVedtak, null);
    });

    it("arver ikke i kjede: både B og C peker på A", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              evaluering: evaluering({ diagnose1: "X01" }),
            }),
            vedtak({ vedtakId: 2, tidspunktReg: "2024-02-01T10:00:00" }),
            vedtak({ vedtakId: 3, tidspunktReg: "2024-03-01T10:00:00" }),
          ],
        }),
      ]);
      const kilde = { vedtakId: 1, vedtakstidspunkt: "2024-01-01T10:00:00" };
      assert.deepEqual(finn(rader, 2).evalueringFraVedtak, kilde);
      assert.deepEqual(finn(rader, 3).evalueringFraVedtak, kilde);
      assert.deepEqual(finn(rader, 3).evaluering, evaluering({ diagnose1: "X01" }));
    });

    it("overskriver ikke egen evaluering, og evalueringFraVedtak er null", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              evaluering: evaluering({ diagnose1: "X01" }),
            }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-02-01T10:00:00",
              evaluering: evaluering({ diagnose1: "X02" }),
            }),
          ],
        }),
      ]);
      const rad = finn(rader, 2);
      assert.deepEqual(rad.evaluering, evaluering({ diagnose1: "X02" }));
      assert.equal(rad.evalueringFraVedtak, null);
    });
  });

  describe("arv av gh", () => {
    it("arver gh fra nærmeste tidligere vedtak med gh", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              gh: [gh({ trygdetid: 10 })],
            }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-02-01T10:00:00",
              gh: [gh({ trygdetid: 20 })],
            }),
            vedtak({ vedtakId: 3, tidspunktReg: "2024-03-01T10:00:00" }),
          ],
        }),
      ]);
      const rad = finn(rader, 3);
      assert.deepEqual(rad.gh, [gh({ trygdetid: 20 })]);
      assert.deepEqual(rad.ghFraVedtak, {
        vedtakId: 2,
        vedtakstidspunkt: "2024-02-01T10:00:00",
      });
    });

    it("arver ikke fra senere vedtak", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" }),
            vedtak({ vedtakId: 2, tidspunktReg: "2024-02-01T10:00:00", gh: [gh()] }),
          ],
        }),
      ]);
      const rad = finn(rader, 1);
      assert.deepEqual(rad.gh, []);
      assert.equal(rad.ghFraVedtak, null);
    });

    it("arver ikke fra vedtak på en annen stønad", () => {
      const rader = lagVedtaksrader([
        stønad({
          stonadId: 1,
          vedtak: [vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" })],
        }),
        stønad({
          stonadId: 2,
          vedtak: [vedtak({ vedtakId: 2, tidspunktReg: "2023-12-01T10:00:00", gh: [gh()] })],
        }),
      ]);
      const rad = finn(rader, 1);
      assert.deepEqual(rad.gh, []);
      assert.equal(rad.ghFraVedtak, null);
    });

    it("arver ikke fra vedtak med samme tidspunktReg", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({ vedtakId: 1, tidspunktReg: "2024-01-01T10:00:00" }),
            vedtak({ vedtakId: 2, tidspunktReg: "2024-01-01T10:00:00", gh: [gh()] }),
          ],
        }),
      ]);
      const rad = finn(rader, 1);
      assert.deepEqual(rad.gh, []);
      assert.equal(rad.ghFraVedtak, null);
    });

    it("arver ikke i kjede: både B og C peker på A", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              gh: [gh({ trygdetid: 10 })],
            }),
            vedtak({ vedtakId: 2, tidspunktReg: "2024-02-01T10:00:00" }),
            vedtak({ vedtakId: 3, tidspunktReg: "2024-03-01T10:00:00" }),
          ],
        }),
      ]);
      const kilde = { vedtakId: 1, vedtakstidspunkt: "2024-01-01T10:00:00" };
      assert.deepEqual(finn(rader, 2).ghFraVedtak, kilde);
      assert.deepEqual(finn(rader, 3).ghFraVedtak, kilde);
      assert.deepEqual(finn(rader, 3).gh, [gh({ trygdetid: 10 })]);
    });

    it("overskriver ikke egen gh, og ghFraVedtak er null", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              gh: [gh({ trygdetid: 10 })],
            }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-02-01T10:00:00",
              gh: [gh({ trygdetid: 20 })],
            }),
          ],
        }),
      ]);
      const rad = finn(rader, 2);
      assert.deepEqual(rad.gh, [gh({ trygdetid: 20 })]);
      assert.equal(rad.ghFraVedtak, null);
    });

    it("arver gh uavhengig av evaluering, og de kan komme fra ulike vedtak", () => {
      const rader = lagVedtaksrader([
        stønad({
          vedtak: [
            vedtak({
              vedtakId: 1,
              tidspunktReg: "2024-01-01T10:00:00",
              evaluering: evaluering({ diagnose1: "X01" }),
            }),
            vedtak({
              vedtakId: 2,
              tidspunktReg: "2024-02-01T10:00:00",
              gh: [gh({ trygdetid: 20 })],
            }),
            vedtak({ vedtakId: 3, tidspunktReg: "2024-03-01T10:00:00" }),
          ],
        }),
      ]);
      const rad = finn(rader, 3);
      assert.deepEqual(rad.evalueringFraVedtak, {
        vedtakId: 1,
        vedtakstidspunkt: "2024-01-01T10:00:00",
      });
      assert.deepEqual(rad.ghFraVedtak, {
        vedtakId: 2,
        vedtakstidspunkt: "2024-02-01T10:00:00",
      });
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

describe("medDataFraTidligereVedtak", () => {
  it("gir tom liste for tom liste", () => {
    assert.deepEqual(medDataFraTidligereVedtak([]), []);
  });

  it("arver evaluering og gh fra tidligere rad på samme stønad", () => {
    const tidligere = enRad(
      vedtak({
        vedtakId: 1,
        tidspunktReg: "2024-01-01T10:00:00",
        evaluering: evaluering(),
        gh: [gh()],
      })
    );
    const senere = enRad(vedtak({ vedtakId: 2, tidspunktReg: "2024-02-01T10:00:00" }));

    const rad = finn(medDataFraTidligereVedtak([tidligere, senere]), 2);
    assert.deepEqual(rad.evaluering, evaluering());
    assert.deepEqual(rad.evalueringFraVedtak, {
      vedtakId: 1,
      vedtakstidspunkt: "2024-01-01T10:00:00",
    });
    assert.deepEqual(rad.gh, [gh()]);
    assert.deepEqual(rad.ghFraVedtak, {
      vedtakId: 1,
      vedtakstidspunkt: "2024-01-01T10:00:00",
    });
  });
});
