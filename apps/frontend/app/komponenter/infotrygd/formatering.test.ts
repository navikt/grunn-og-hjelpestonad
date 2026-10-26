import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { formaterDato, formaterMaaned, formaterPeriode } from "./formatering.ts";

describe("formaterMaaned", () => {
  it("gir måned og år på norsk", () => {
    assert.equal(formaterMaaned("2024-03"), "mars 2024");
    assert.equal(formaterMaaned("2024-01"), "januar 2024");
  });

  it("returnerer verdien uendret når måneden er utenfor 01-12", () => {
    assert.equal(formaterMaaned("2024-13"), "2024-13");
    assert.equal(formaterMaaned("2024-00"), "2024-00");
  });

  it("returnerer verdien uendret når formatet ikke er ÅÅÅÅ-MM", () => {
    assert.equal(formaterMaaned("abc"), "abc");
    assert.equal(formaterMaaned("2024-3"), "2024-3");
  });
});

describe("formaterDato", () => {
  it("gir dd.MM.yyyy", () => {
    assert.equal(formaterDato("2024-03-05"), "05.03.2024");
  });

  it("returnerer verdien uendret når datoen er ugyldig", () => {
    assert.equal(formaterDato("ikke-en-dato"), "ikke-en-dato");
  });
});

describe("formaterPeriode", () => {
  it("skriver fra og til med tankestrek mellom", () => {
    assert.equal(formaterPeriode("2024-03-05", "2024-04-10"), "05.03.2024 – 10.04.2024");
  });

  it("lar perioden stå åpen når tom er null eller undefined", () => {
    assert.equal(formaterPeriode("2024-03-05", null), "05.03.2024 –");
    assert.equal(formaterPeriode("2024-03-05", undefined), "05.03.2024 –");
  });
});
