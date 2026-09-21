import type {
  VilkårDiagnoseRequest,
  VilkårDiagnoseResponse,
  VilkårInstitusjonRequest,
  VilkårInstitusjonResponse,
  VilkårMedlemskapRequest,
  VilkårMedlemskapResponse,
} from "~/api/generated/types.gen";

export type {
  VilkårDiagnoseRequest,
  VilkårDiagnoseResponse,
  VilkårInstitusjonRequest,
  VilkårInstitusjonResponse,
  VilkårMedlemskapRequest,
  VilkårMedlemskapResponse,
};

export const vilkårNøkler = ["medlemskap", "diagnose", "institusjon"] as const;

export type VilkårNøkkel = (typeof vilkårNøkler)[number];

export const vilkårNavn: Record<VilkårNøkkel, string> = {
  medlemskap: "Medlemskap i folketrygden",
  diagnose: "Sykdom, skade eller lyte",
  institusjon: "Opphold i institusjon",
};

export const vilkårHjemmel: Record<VilkårNøkkel, string> = {
  medlemskap: "Folketrygdloven § 2-1 flg., jf. § 6-1 a",
  diagnose: "Folketrygdloven § 6-2",
  institusjon: "Folketrygdloven § 6-8",
};

export const vilkårSpørsmål: Record<VilkårNøkkel, string> = {
  medlemskap: "Er bruker medlem i folketrygden eller omfattet av EØS-forordningen i perioden?",
  diagnose: "Har bruker en varig sykdom, skade eller lyte i perioden?",
  institusjon: "Er vilkåret om at bruker ikke oppholder seg i institusjon oppfylt i perioden?",
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

export type Oppholdstype = NonNullable<VilkårInstitusjonResponse["oppholdstype"]>;

export const oppholdstypeTekst: Record<Oppholdstype, string> = {
  HELSE_OG_OMSORGSINSTITUSJON: "Helse- og omsorgsinstitusjon",
  SPESIALISTHELSETJENESTEN: "Spesialisthelsetjenesten",
  PSYKISK_HELSEVERN: "Psykisk helsevern",
  FENGSEL_ELLER_ANNEN_LOVREGULERT_BOFORM: "Fengsel eller annen lovregulert boform",
};

export type Unntakshjemmel = NonNullable<VilkårInstitusjonResponse["unntakshjemmel"]>;

export const unntakshjemmelTekst: Record<Unntakshjemmel, string> = {
  EKSTRAUTGIFTER_IKKE_DEKKET_AV_INSTITUSJONEN: "Ekstrautgifter ikke dekket av institusjonen",
  KORTTIDSOPPHOLD: "Korttidsopphold",
  BARN_UNDER_18_I_SPESIALISTHELSETJENESTEN: "Barn under 18 år i spesialisthelsetjenesten",
  ANNET_UNNTAK_I_FORSKRIFT: "Annet unntak i forskrift",
};
