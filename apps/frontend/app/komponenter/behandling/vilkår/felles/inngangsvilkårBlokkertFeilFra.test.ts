import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { inngangsvilkårBlokkertFeilFra } from "./inngangsvilkårBlokkertFeilFra.ts";

describe("inngangsvilkårBlokkertFeilFra", () => {
  it("returnerer feilen når backend svarer med FALSK_IDENTITET", () => {
    const blokkertFeilFraBackend = {
      feilkode: "FALSK_IDENTITET",
      melding: "Behandlingen kan ikke fullføres på grunn av falsk identitet.",
      status: 422,
    };

    assert.deepEqual(inngangsvilkårBlokkertFeilFra(blokkertFeilFraBackend), blokkertFeilFraBackend);
  });

  it("returnerer undefined når feilkode er ukjent", () => {
    const feilMedUkjentFeilkode = {
      feilkode: "ANNEN_FEIL",
      melding: "Noe annet blokkerer behandlingen.",
      status: 422,
    };

    assert.equal(inngangsvilkårBlokkertFeilFra(feilMedUkjentFeilkode), undefined);
  });

  it("returnerer undefined for valideringsfeil fra vilkårssteget (400)", () => {
    const valideringsfeilResponse = {
      melding: "Valideringsfeil",
      status: 400,
      feil: [{ nøkkel: "DIAGNOSE", melding: "Diagnose må vurderes." }],
    };

    assert.equal(inngangsvilkårBlokkertFeilFra(valideringsfeilResponse), undefined);
  });

  it("returnerer undefined for generisk feilrespons", () => {
    assert.equal(
      inngangsvilkårBlokkertFeilFra({ melding: "Noe gikk galt.", status: 500 }),
      undefined
    );
  });

  it("returnerer undefined for null, undefined og Error", () => {
    assert.equal(inngangsvilkårBlokkertFeilFra(null), undefined);
    assert.equal(inngangsvilkårBlokkertFeilFra(undefined), undefined);
    assert.equal(inngangsvilkårBlokkertFeilFra(new Error("Nettverksfeil")), undefined);
  });
});
