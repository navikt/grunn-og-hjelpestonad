import type { Valideringsfeil as ValideringsfeilResponse } from "~/api/generated/types.gen";
import { zVilkårValideringFeilResponse } from "~/api/generated/zod.gen";
import type { VilkårNøkkel } from "~/types/vilkår";

export interface Valideringsfeil {
  nøkkel: VilkårNøkkel;
  melding: string;
}

const nøkkelForVilkårType: Record<ValideringsfeilResponse["vilkårType"], VilkårNøkkel> = {
  MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN: "medlemskap",
  DIAGNOSE: "diagnose",
  IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM: "institusjon",
};

/**
 * Backend svarer 400 med `VilkårValideringFeilResponse` når vilkårssteget ikke kan fullføres.
 * Andre feil (500, nettverk) har en annen form, så vi parser før vi mapper til vilkårsnøkler.
 */
export function valideringsfeilFra(feil: unknown): Valideringsfeil[] {
  const resultat = zVilkårValideringFeilResponse.safeParse(feil);
  if (!resultat.success) {
    return [];
  }

  return resultat.data.feil.map(({ vilkårType, melding }) => ({
    nøkkel: nøkkelForVilkårType[vilkårType],
    melding,
  }));
}
