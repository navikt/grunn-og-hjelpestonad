import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { finnLokalClientId } from "./local-auth.ts";

describe("finnLokalClientId", () => {
  it("velger beslutter-klient når bruker er beslutter", () => {
    assert.equal(finnLokalClientId("beslutter", "test"), "beslutter");
  });

  it("faller tilbake til standard clientId for saksbehandler og andre verdier", () => {
    assert.equal(finnLokalClientId("saksbehandler", "test"), "test");
    assert.equal(finnLokalClientId("ukjent", "test"), "test");
    assert.equal(finnLokalClientId(undefined, "test"), "test");
  });
});
