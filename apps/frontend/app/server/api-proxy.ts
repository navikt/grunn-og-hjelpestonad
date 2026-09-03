import type { Request, Response } from "express";
import { hentAccessToken } from "./utils/token.js";
import { exchangeTokenForBackend } from "./obo-token-exchange.js";

const GJENLEVENDE_BS_SAK_AUDIENCE = "api://dev-gcp.etterlatte.gjenlevende-bs-sak/.default";

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
    console.error("Ingen token funnet. erLokalt:", erLokalt);
    return;
  }

  if (erLokalt) {
    return token;
  }

  try {
    return await exchangeTokenForBackend(token, audience);
  } catch (error) {
    console.error("OBO token exchange feilet:", error);
    throw error;
  }
};

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
  audience = GJENLEVENDE_BS_SAK_AUDIENCE,
  backendApiPrefix = "/api"
) {
  return async (req: Request, res: Response) => {
    let token: string | undefined;

    try {
      token = await hentTokenForBackend(req, erLokalt, audience);
    } catch (error) {
      const errorMelding = error instanceof Error ? error.message : "Ukjent feil";
      console.error("Token-utveksling feilet:", error);
      res.status(401).json({ error: "Token-utveksling feilet", melding: errorMelding });
      return;
    }

    if (!token) {
      res.status(401).json({ error: "Ikke autentisert" });
      return;
    }

    try {
      const url = byggBackendUrl(backendUrl, req, backendApiPrefix);
      console.log("Proxying request til:", url);

      const backendResponse = await kallBackend(url, req, token);
      console.log("Backend response status:", backendResponse.status);

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
        console.error(
          `Backend returnerte uventet Content-Type: ${contentType || "ukjent"} (HTTP ${backendResponse.status})`,
          body.substring(0, 500)
        );
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
      const url = byggBackendUrl(backendUrl, req, backendApiPrefix);

      console.error(`Kall til backend feilet [${req.method} ${url}]:`, error);
      if (error instanceof Error && error.cause) {
        console.error("Rotårsak:", error.cause);
      }

      res.status(500).json({ error: "Feil ved kall til backend", melding: errorMelding });
    }
  };
}
