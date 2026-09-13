import type { VilkårVurderingDto } from "~/api/generated/types.gen";

export type VilkårType = VilkårVurderingDto["vilkårType"];

export const Vurdering = {
  JA: "JA",
  NEI: "NEI",
} as const;

export type Vurdering = VilkårVurderingDto["vurdering"];
