import { apiCall } from "./backend";

export interface SakId {
  sakId: number;
}

export async function hentEtterlatteSakIdMedPersonident(args: { fnr: string }) {
  return apiCall<SakId>(`/etterlatte-behandling/personer/sakid`, {
    method: "POST",
    body: JSON.stringify({ foedselsnummer: args.fnr }),
  });
}
