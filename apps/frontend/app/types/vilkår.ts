import type { VilkårVurderingResponse } from "~/api/generated/types.gen";

export type VilkårType = VilkårVurderingResponse["vilkårType"];

export const Vurdering = {
  JA: "JA",
  NEI: "NEI",
} as const;

export type Vurdering = VilkårVurderingResponse["vurdering"];
