import type {
  DiagnosekodeResponse,
  VilkårDiagnoseRequest,
  VilkårDiagnoseResponse,
  VilkårMedlemskapRequest,
  VilkårMedlemskapResponse,
} from "~/api/generated/types.gen";

export type {
  DiagnosekodeResponse,
  VilkårDiagnoseRequest,
  VilkårDiagnoseResponse,
  VilkårMedlemskapRequest,
  VilkårMedlemskapResponse,
};

export const vilkårNøkler = ["medlemskap", "diagnose"] as const;

export type VilkårNøkkel = (typeof vilkårNøkler)[number];

export const vilkårNavn: Record<VilkårNøkkel, string> = {
  medlemskap: "Medlemskap i folketrygden",
  diagnose: "Sykdom, skade eller lyte",
};

export const vilkårHjemmel: Record<VilkårNøkkel, string> = {
  medlemskap: "Folketrygdloven § 2-1 flg., jf. § 6-1 a",
  diagnose: "Folketrygdloven § 6-2",
};

export const vilkårSpørsmål: Record<VilkårNøkkel, string> = {
  medlemskap: "Er bruker medlem i folketrygden eller omfattet av EØS-forordningen i perioden?",
  diagnose: "Har bruker en varig sykdom, skade eller lyte i perioden?",
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
