import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { beregnAlder } from "./utils.ts";

// Fødselsdato som YYYY-MM-DD, et antall år og dager fra i dag.
function fødselsdato(årSiden: number, dagerFraIdag = 0): string {
  const idag = new Date();
  const dato = new Date(
    idag.getFullYear() - årSiden,
    idag.getMonth(),
    idag.getDate() + dagerFraIdag
  );
  const mm = String(dato.getMonth() + 1).padStart(2, "0");
  const dd = String(dato.getDate()).padStart(2, "0");
  return `${dato.getFullYear()}-${mm}-${dd}`;
}

describe("beregnAlder", () => {
  it("teller bursdagen i dag som fylt", () => {
    assert.equal(beregnAlder(fødselsdato(30)), 30);
  });

  it("teller bursdagen i går som fylt", () => {
    assert.equal(beregnAlder(fødselsdato(30, -1)), 30);
  });

  it("teller ikke bursdagen i morgen som fylt", () => {
    assert.equal(beregnAlder(fødselsdato(30, 1)), 29);
  });

  it("teller ikke bursdagen neste måned som fylt", () => {
    assert.equal(beregnAlder(fødselsdato(30, 40)), 29);
  });

  it("gir null for manglende eller ugyldig dato", () => {
    assert.equal(beregnAlder(null), null);
    assert.equal(beregnAlder(undefined), null);
    assert.equal(beregnAlder(""), null);
    assert.equal(beregnAlder("ikke-en-dato"), null);
  });
});
