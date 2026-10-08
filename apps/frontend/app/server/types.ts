import type { AppEnv } from "./env.js";

export interface Saksbehandler {
  navn: string;
  epost: string;
  navIdent: string;
  accessToken?: string;
}

export interface Oppsett {
  saksbehandler: Saksbehandler | null;
  env: AppEnv;
  telemetryCollectorUrl?: string;
}
