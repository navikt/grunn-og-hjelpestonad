import type { Valideringsfeil } from "~/api/generated/types.gen";
import { zVilkårValideringFeilResponse } from "~/api/generated/zod.gen";

export type { Valideringsfeil };
export type Valideringsnøkkel = Valideringsfeil["nøkkel"];

/**
 * Begge stegene bruker samme feilformat. Andre feil går til generell feilvisning.
 */
export function valideringsfeilFra(feil: unknown): Valideringsfeil[] {
  const resultat = zVilkårValideringFeilResponse.safeParse(feil);
  return resultat.success ? resultat.data.feil : [];
}
