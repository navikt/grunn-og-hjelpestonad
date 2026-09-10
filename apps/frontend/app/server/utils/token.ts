import type { Request } from "express";
import { getToken, parseAzureUserToken } from "@navikt/oasis";
import type { Saksbehandler } from "../types.js";
import { structuredLog } from "../structured-log.js";

export function parseToken(token: string): Saksbehandler | undefined {
  const parsed = parseAzureUserToken(token);

  if (!parsed.ok) {
    structuredLog("warn", "user_token_parse_failed");
    return;
  }

  return {
    navn: parsed.name || "",
    epost: parsed.preferred_username || "",
    navIdent: parsed.NAVident || "",
  };
}

export function hentSaksbehandlerFraHeaders(req: Request): Saksbehandler | undefined {
  const token = getToken(req);

  if (!token) {
    return;
  }

  return parseToken(token);
}

export function hentAccessToken(req: Request, erLokalt: boolean): string | undefined {
  if (erLokalt) {
    return req.session.localAuthUser?.accessToken;
  } else {
    const token = getToken(req);

    if (!token) {
      console.error("Ingen Bearer token funnet i Authorization header");
      return undefined;
    }

    return token;
  }
}
