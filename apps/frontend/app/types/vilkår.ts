import type {
  DiagnosekodeResponse,
  Ekstrautgift,
  VilkårDiagnoseRequest,
  VilkårDiagnoseResponse,
  VilkårEkstrautgifterRequest,
  VilkårEkstrautgifterResponse,
  VilkårMedlemskapRequest,
  VilkårMedlemskapResponse,
} from "~/api/generated/types.gen";

export type {
  DiagnosekodeResponse,
  Ekstrautgift,
  VilkårDiagnoseRequest,
  VilkårDiagnoseResponse,
  VilkårEkstrautgifterRequest,
  VilkårEkstrautgifterResponse,
  VilkårMedlemskapRequest,
  VilkårMedlemskapResponse,
};

export const vilkårNøkler = ["medlemskap", "diagnose", "ekstrautgifter"] as const;

export type VilkårNøkkel = (typeof vilkårNøkler)[number];

export const vilkårNavn: Record<VilkårNøkkel, string> = {
  medlemskap: "Medlemskap i folketrygden",
  diagnose: "Sykdom, skade eller lyte",
  ekstrautgifter: "Nødvendige ekstrautgifter",
};

export const vilkårHjemmel: Record<VilkårNøkkel, string> = {
  medlemskap: "Folketrygdloven § 2-1 flg., jf. § 6-1 a",
  diagnose: "Folketrygdloven § 6-2",
  ekstrautgifter: "Folketrygdloven § 6-3",
};

export const vilkårSpørsmål: Record<VilkårNøkkel, string> = {
  medlemskap: "Er bruker medlem i folketrygden eller omfattet av EØS-forordningen i perioden?",
  diagnose: "Har bruker en varig sykdom, skade eller lyte i perioden?",
  ekstrautgifter: "Har bruker nødvendige ekstrautgifter som gir rett til grunnstønad i perioden?",
};

export const Vurdering = {
  JA: "JA",
  NEI: "NEI",
} as const;

export type Vurdering = (typeof Vurdering)[keyof typeof Vurdering];

export type Regelverk = VilkårMedlemskapResponse["regelverk"];

export const regelverkTekst: Record<Regelverk, string> = {
  NASJONALE_REGLER: "Nasjonale regler",
  EØS_FORORDNINGEN: "EØS-forordningen",
};

export type Hjemmelbokstav = Ekstrautgift["hjemmelbokstav"];

export const hjemmelbokstavTekst: Record<Hjemmelbokstav, string> = {
  A: "a) Drift av tekniske hjelpemidler",
  B: "b) Transport, herunder drift av egen bil",
  C: "c) Hold av førerhund",
  D: "d) Telefon",
  E: "e) Proteser, støttebandasje o.l.",
  F: "f) Fordyret kosthold",
  G: "g) Slitasje på klær og sengetøy",
  H: "h) Hold av servicehund",
};

// D (telefon) og E (proteser o.l.) gir ikke grunnstønad i nye tilfeller fra 1. januar 2020.
export const hjemmelbokstaverForNyeTilfeller: Hjemmelbokstav[] = ["A", "B", "C", "F", "G", "H"];

export type Sats = NonNullable<VilkårEkstrautgifterResponse["sats"]>;

export const satsTekst: Record<Sats, string> = {
  SATS_1: "Sats 1",
  SATS_2: "Sats 2",
  SATS_3: "Sats 3",
  SATS_4: "Sats 4",
  SATS_5: "Sats 5",
  SATS_6: "Sats 6",
};
