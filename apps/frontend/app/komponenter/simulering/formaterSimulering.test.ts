import { describe, it } from "node:test";
import assert from "node:assert/strict";
import type { Simuleringsbeløp, Simuleringsperiode } from "~/api/generated/types.gen";
import {
  formaterAntallMåneder,
  formaterBeløpEllerStrek,
  formaterBeløpPerMåned,
  formaterMåned,
  formaterMånedLang,
  formaterOppsummeringTittel,
  formaterPeriode,
  formaterSimulertTidspunkt,
  medStorForbokstav,
  erEtterbetaling,
  erFeilutbetaling,
  harManuellPostering,
  harManuellPosteringIPerioden,
  harManuellPosteringMedResultat,
} from "./formaterSimulering.ts";

function lagBeløp(overstyring: Partial<Simuleringsbeløp> = {}): Simuleringsbeløp {
  return {
    nyttBeløp: 0,
    tidligereUtbetalt: 0,
    resultat: 0,
    feilutbetaling: 0,
    etterbetaling: 0,
    manuellPostering: 0,
    ...overstyring,
  };
}

function lagPeriode(overstyring: Partial<Simuleringsperiode> = {}): Simuleringsperiode {
  return {
    fom: "2026-02-01",
    tom: "2026-11-30",
    antallMåneder: 10,
    erNesteUtbetaling: false,
    beløpPerMåned: lagBeløp(),
    sumForPerioden: lagBeløp(),
    ...overstyring,
  };
}

describe("formaterMåned", () => {
  it("gir kort månedsnavn og år", () => {
    assert.equal(formaterMåned("2026-02-01"), "feb. 2026");
    assert.equal(formaterMåned("2026-12-31"), "des. 2026");
    assert.equal(formaterMåned("2026-05-01"), "mai 2026");
  });

  it("returnerer verdien uendret når datoen ikke er ÅÅÅÅ-MM-DD", () => {
    assert.equal(formaterMåned("ikke-en-dato"), "ikke-en-dato");
    assert.equal(formaterMåned("2026-13-01"), "2026-13-01");
  });
});

describe("formaterMånedLang", () => {
  it("gir langt månedsnavn og år med liten forbokstav", () => {
    assert.equal(formaterMånedLang("2026-12-01"), "desember 2026");
    assert.equal(formaterMånedLang("2026-02-01"), "februar 2026");
  });

  it("returnerer verdien uendret når datoen ikke er ÅÅÅÅ-MM-DD", () => {
    assert.equal(formaterMånedLang("ikke-en-dato"), "ikke-en-dato");
    assert.equal(formaterMånedLang("2026-13-01"), "2026-13-01");
  });
});

describe("medStorForbokstav", () => {
  it("gjør første bokstav stor og lar resten være", () => {
    assert.equal(medStorForbokstav("desember 2026"), "Desember 2026");
    assert.equal(medStorForbokstav("Desember 2026"), "Desember 2026");
  });

  it("gir tom tekst uendret", () => {
    assert.equal(medStorForbokstav(""), "");
  });
});

describe("formaterAntallMåneder", () => {
  it("skiller entall og flertall", () => {
    assert.equal(formaterAntallMåneder(1), "1 måned");
    assert.equal(formaterAntallMåneder(10), "10 måneder");
  });
});

describe("formaterPeriode", () => {
  it("skriver fra og til når perioden er flere måneder", () => {
    assert.equal(formaterPeriode("2026-02-01", "2026-11-30", 10), "feb. 2026–nov. 2026");
  });

  it("skriver bare én måned når perioden er én måned", () => {
    assert.equal(formaterPeriode("2026-12-01", "2026-12-31", 1), "des. 2026");
  });
});

describe("formaterOppsummeringTittel", () => {
  it("er «Totalt» når fom eller tom mangler", () => {
    assert.equal(formaterOppsummeringTittel(null, "2026-11-30"), "Totalt");
    assert.equal(formaterOppsummeringTittel("2026-02-01", undefined), "Totalt");
    assert.equal(formaterOppsummeringTittel(null, null), "Totalt");
  });

  it("nevner måneden når perioden er innenfor én måned", () => {
    assert.equal(formaterOppsummeringTittel("2026-02-01", "2026-02-28"), "Totalt for februar 2026");
  });

  it("skriver datoene når perioden går over flere måneder", () => {
    assert.equal(
      formaterOppsummeringTittel("2026-02-01", "2026-11-30"),
      "Totalt for perioden 01.02.2026 – 30.11.2026"
    );
  });
});

describe("formaterBeløpEllerStrek", () => {
  it("gir «–» når beløpet er 0", () => {
    assert.equal(formaterBeløpEllerStrek(0), "–");
  });

  it("skriver beløpet med «kr» når beløpet er ulik 0", () => {
    assert.equal(formaterBeløpEllerStrek(752), "752 kr");
  });
});

describe("formaterBeløpPerMåned", () => {
  it("skriver beløpet med «per måned»", () => {
    assert.equal(formaterBeløpPerMåned(752), "752 kr per måned");
  });

  it("gir negativt beløp et synlig minustegn", () => {
    assert.equal(formaterBeløpPerMåned(-752), "−752 kr per måned");
  });
});

describe("formaterSimulertTidspunkt", () => {
  it("skriver dato og klokkeslett", () => {
    assert.equal(formaterSimulertTidspunkt("2026-11-19T14:05:00"), "19.11.2026 kl.14:05");
  });

  it("gir «–» når tidspunkt mangler", () => {
    assert.equal(formaterSimulertTidspunkt(null), "–");
    assert.equal(formaterSimulertTidspunkt(undefined), "–");
  });
});

describe("harManuellPostering", () => {
  it("er false når ingen periode har manuell postering", () => {
    assert.equal(harManuellPostering([lagPeriode(), lagPeriode()]), false);
    assert.equal(harManuellPostering([]), false);
  });

  it("er true når minst én periode har manuell postering", () => {
    const perioderMedManuell = [
      lagPeriode(),
      lagPeriode({ beløpPerMåned: lagBeløp({ manuellPostering: 150 }) }),
    ];
    assert.equal(harManuellPostering(perioderMedManuell), true);
  });
});

describe("harManuellPosteringMedResultat", () => {
  it("er false når perioden med manuell postering har resultat 0", () => {
    const perioder = [
      lagPeriode({ beløpPerMåned: lagBeløp({ manuellPostering: 150, resultat: 0 }) }),
    ];
    assert.equal(harManuellPosteringMedResultat(perioder), false);
  });

  it("er true når samme periode har både manuell postering og resultat", () => {
    const perioder = [
      lagPeriode({ beløpPerMåned: lagBeløp({ resultat: 752 }) }),
      lagPeriode({ beløpPerMåned: lagBeløp({ manuellPostering: -50, resultat: 702 }) }),
    ];
    assert.equal(harManuellPosteringMedResultat(perioder), true);
  });

  it("er false når manuell postering og resultat er i hver sin periode", () => {
    const perioder = [
      lagPeriode({ beløpPerMåned: lagBeløp({ manuellPostering: 150, resultat: 0 }) }),
      lagPeriode({ beløpPerMåned: lagBeløp({ resultat: 752 }) }),
    ];
    assert.equal(harManuellPosteringMedResultat(perioder), false);
  });
});

describe("erEtterbetaling", () => {
  it("er sann bare når perioden har etterbetaling per måned", () => {
    assert.equal(
      erEtterbetaling(lagPeriode({ beløpPerMåned: lagBeløp({ etterbetaling: 752 }) })),
      true
    );
    assert.equal(
      erEtterbetaling(lagPeriode({ beløpPerMåned: lagBeløp({ nyttBeløp: 752, resultat: 752 }) })),
      false
    );
  });
});

describe("erFeilutbetaling", () => {
  it("er sann bare når perioden har feilutbetaling per måned", () => {
    assert.equal(
      erFeilutbetaling(
        lagPeriode({ beløpPerMåned: lagBeløp({ feilutbetaling: 752, resultat: -752 }) })
      ),
      true
    );
    assert.equal(erFeilutbetaling(lagPeriode()), false);
  });
});

describe("harManuellPosteringIPerioden", () => {
  it("er sann for både positive og negative manuelle posteringer", () => {
    assert.equal(
      harManuellPosteringIPerioden(
        lagPeriode({ beløpPerMåned: lagBeløp({ manuellPostering: 165 }) })
      ),
      true
    );
    assert.equal(
      harManuellPosteringIPerioden(
        lagPeriode({ beløpPerMåned: lagBeløp({ manuellPostering: -50 }) })
      ),
      true
    );
    assert.equal(harManuellPosteringIPerioden(lagPeriode()), false);
  });
});
