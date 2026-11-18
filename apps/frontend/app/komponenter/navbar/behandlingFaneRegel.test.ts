import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { erFaneLåst, visningsnavnForNåværendeSteg } from "./behandlingFaneRegel.ts";

describe("erFaneLåst", () => {
  it("låser alle faner unntatt årsak før behandlingen er lastet", () => {
    assert.equal(erFaneLåst("ÅRSAK", undefined), false);
    assert.equal(erFaneLåst("INNGANGSVILKÅR", undefined), true);
    assert.equal(erFaneLåst("VEDTAK_OG_BREV", undefined), true);
  });

  it("åpner faner til og med nåværendeSteg og låser senere steg når behandlingen er redigerbar", () => {
    const behandling = { status: "UTREDES" as const, nåværendeSteg: "INNGANGSVILKÅR" as const };

    assert.equal(erFaneLåst("ÅRSAK", behandling), false);
    assert.equal(erFaneLåst("INNGANGSVILKÅR", behandling), false);
    assert.equal(erFaneLåst("SATS_OG_TRYGDETID", behandling), true);
    assert.equal(erFaneLåst("VEDTAK_OG_BREV", behandling), true);
  });

  it("åpner alle faner når behandlingen ikke er redigerbar", () => {
    const behandling = { status: "FATTER_VEDTAK" as const, nåværendeSteg: "ÅRSAK" as const };

    assert.equal(erFaneLåst("INNGANGSVILKÅR", behandling), false);
    assert.equal(erFaneLåst("VEDTAK_OG_BREV", behandling), false);
  });
});

describe("visningsnavnForNåværendeSteg", () => {
  it("bruker samme visningsnavn som backend", () => {
    assert.equal(visningsnavnForNåværendeSteg("VEDTAK_OG_BREV"), "Simulering og brev");
    assert.equal(visningsnavnForNåværendeSteg(undefined), "Årsak behandling");
  });
});
