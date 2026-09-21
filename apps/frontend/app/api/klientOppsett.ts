import { client } from "~/api/generated/client.gen";
import { varsleManglerTilgang } from "~/utils/manglerTilgangEvent";

/**
 * Registrerer 403-interceptoren på den genererte klienten. Fetch-klienten tar ikke
 * interceptors gjennom `createClientConfig`, så de må settes på klientinstansen etter at
 * den er opprettet. Speiler oppførselen i `apiCall` i `app/api/backend.ts`.
 *
 * Kjører kun i nettleseren. Klienten er bevisst nettleser-only: `baseUrl` er relativ
 * (`/api`), og express-proxyen veksler token basert på brukerens innkommende request.
 * Ingen av delene finnes under SSR, så all datahenting må skje etter hydrering.
 */
let erRegistrert = false;

export function registrerApiInterceptorer(): void {
  if (erRegistrert || typeof window === "undefined") {
    return;
  }
  erRegistrert = true;

  client.interceptors.response.use((response) => {
    if (response.status === 403) {
      varsleManglerTilgang();
    }
    return response;
  });
}
