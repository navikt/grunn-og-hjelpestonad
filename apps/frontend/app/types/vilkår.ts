export type VilkårType =
  | "INNGANGSVILKÅR"
  | "AKTIVITET"
  | "INNTEKT"
  | "ALDER_PÅ_BARN"
  | "DOKUMENTASJON_TILSYNSUTGIFTER";

export const Vurdering = {
  JA: "JA",
  NEI: "NEI",
} as const;

export type Vurdering = (typeof Vurdering)[keyof typeof Vurdering];
