import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { lagDødsfallVarselTekst } from "./dødsfallVarselTekst.ts";

const TITTEL = "Personen er registrert død";

describe("lagDødsfallVarselTekst", () => {
  it("returnerer undefined når listen er tom", () => {
    assert.equal(lagDødsfallVarselTekst([]), undefined);
  });

  it("gir én dødsdato når det finnes ett dødsfall med dato", () => {
    const dødsfallVarselTekst = lagDødsfallVarselTekst([
      { beskrivelse: "Dødsfall", fraOgMedDato: "2026-09-01" },
    ]);

    assert.deepEqual(dødsfallVarselTekst, {
      tittel: TITTEL,
      beskrivelse:
        "Folkeregisteret viser dødsdato 01.09.2026. Avslutt medlemskapsperioden på dødsdatoen.",
    });
  });

  it("gir melding om registrert dødsfall uten dato når fraOgMedDato er null eller undefined", () => {
    const meldingUtenDato =
      "Folkeregisteret har registrert dødsfall, men ikke dato. Avslutt medlemskapsperioden når du har funnet dødsdatoen.";

    assert.deepEqual(lagDødsfallVarselTekst([{ beskrivelse: "Dødsfall", fraOgMedDato: null }]), {
      tittel: TITTEL,
      beskrivelse: meldingUtenDato,
    });
    assert.deepEqual(lagDødsfallVarselTekst([{ beskrivelse: "Dødsfall" }]), {
      tittel: TITTEL,
      beskrivelse: meldingUtenDato,
    });
  });

  it("lister flere dødsdatoer i rekkefølgen fra backend når datoene er ulike", () => {
    const dødsfallVarselTekst = lagDødsfallVarselTekst([
      { beskrivelse: "Dødsfall", fraOgMedDato: "2026-09-01" },
      { beskrivelse: "Dødsfall", fraOgMedDato: "2026-08-15" },
    ]);

    assert.deepEqual(dødsfallVarselTekst, {
      tittel: TITTEL,
      beskrivelse:
        "Folkeregisteret viser flere dødsdatoer: 01.09.2026, 15.08.2026. Kontroller hvilken som gjelder, og avslutt medlemskapsperioden på dødsdatoen.",
    });
  });

  it("dedupliserer flere dødsfall med samme dato til én dato", () => {
    const dødsfallVarselTekst = lagDødsfallVarselTekst([
      { beskrivelse: "Dødsfall", fraOgMedDato: "2026-09-01" },
      { beskrivelse: "Dødsfall", fraOgMedDato: "2026-09-01" },
    ]);

    assert.deepEqual(dødsfallVarselTekst, {
      tittel: TITTEL,
      beskrivelse:
        "Folkeregisteret viser dødsdato 01.09.2026. Avslutt medlemskapsperioden på dødsdatoen.",
    });
  });
});
