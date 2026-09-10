import type { Request, Response } from "express";
import { hentAccessToken } from "./utils/token.js";
import { exchangeTokenForBackend } from "./obo-token-exchange.js";
import { structuredLog } from "./structured-log.js";

const GRUNN_OG_HJELPESTONAD_AUDIENCE =
  "api://dev-gcp.grunn-og-hjelp.grunn-og-hjelpestonad/.default";

const byggBackendUrl = (backendUrl: string, req: Request, backendApiPrefix: string): string => {
  return `${backendUrl}${backendApiPrefix}${req.url}`;
};

const hentTokenForBackend = async (
  req: Request,
  erLokalt: boolean,
  audience: string
): Promise<string | undefined> => {
  const token = hentAccessToken(req, erLokalt);

  if (!token) {
    structuredLog("warn", "backend_token_missing", {
      local_environment: erLokalt,
    });
    return;
  }

  if (erLokalt) {
    return token;
  }

  return exchangeTokenForBackend(token, audience);
}

const kallBackend = async (url: string, req: Request, token: string) => {
  const harBody = req.method.toUpperCase() === "POST";

  return await fetch(url, {
    method: req.method,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: harBody ? JSON.stringify(req.body) : undefined,
  });
};

export function lagApiProxy(
  backendUrl: string,
  erLokalt: boolean,
  audience = GRUNN_OG_HJELPESTONAD_AUDIENCE,
  backendApiPrefix = "/api"
) {
  return async (req: Request, res: Response) => {
    let token: string | undefined;

    try {
      token = await hentTokenForBackend(req, erLokalt, audience);
    } catch (error) {
      const errorMelding = error instanceof Error ? error.message : "Ukjent feil";
      structuredLog("error", "backend_token_exchange_failed", {
        error_type: error instanceof Error ? error.name : "unknown",
      });
      res.status(401).json({ error: "Token-utveksling feilet", melding: errorMelding });
      return;
    }

    if (!token) {
      res.status(401).json({ error: "Ikke autentisert" });
      return;
    }

    try {
      const url = byggBackendUrl(backendUrl, req, backendApiPrefix);

      const backendResponse = await kallBackend(url, req, token);

      if (backendResponse.status === 204) {
        res.status(204).end();
        return;
      }

      const contentType = backendResponse.headers.get("Content-Type") ?? "";

      const erPdfRespons = contentType.includes("application/pdf");

      if (erPdfRespons) {
        res.status(backendResponse.status);
        res.setHeader("Content-Type", contentType);
        const contentDisposition = backendResponse.headers.get("Content-Disposition");
        if (contentDisposition) {
          res.setHeader("Content-Disposition", contentDisposition);
        }
        const buffer = await backendResponse.arrayBuffer();
        res.end(Buffer.from(buffer));
        return;
      }

      if (!contentType.includes("application/json")) {
        const body = await backendResponse.text();
        structuredLog("error", "backend_unexpected_content_type", {
          content_type: contentType || "unknown",
          status: backendResponse.status,
          body_length: body.length,
        });
        res.status(backendResponse.status).json({
          error: "Uventet svar fra backend",
          melding: `Forventet JSON, men mottok ${contentType || "ukjent innholdstype"} (HTTP ${backendResponse.status})`,
        });
        return;
      }

      const data = await backendResponse.json();
      res.status(backendResponse.status).send(data);
    } catch (error) {
      const errorMelding = error instanceof Error ? error.message : "Ukjent feil";

      structuredLog("error", "backend_request_failed", {
        error_type: error instanceof Error ? error.name : "unknown",
        method: req.method,
      });

      res.status(500).json({ error: "Feil ved kall til backend", melding: errorMelding });
    }
  };
}
