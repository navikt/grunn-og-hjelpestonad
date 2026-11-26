import type { InngangsvilkårBlokkertResponse } from "~/api/generated/types.gen";
import { zInngangsvilkårBlokkertResponse } from "~/api/generated/zod.gen";

export type InngangsvilkårFeilkode = InngangsvilkårBlokkertResponse["feilkode"];
export type InngangsvilkårBlokkertFeil = InngangsvilkårBlokkertResponse;

/**
 * Backend svarer med 422 og `InngangsvilkårBlokkertResponse` når inngangsvilkårene ikke kan
 * fullføres, for eksempel ved falsk identitet. Andre feil, også valideringsfeil (400), gir `undefined`.
 */
export function inngangsvilkårBlokkertFeilFra(
  feil: unknown
): InngangsvilkårBlokkertFeil | undefined {
  const parseResultat = zInngangsvilkårBlokkertResponse.safeParse(feil);
  return parseResultat.success ? parseResultat.data : undefined;
}
