import path from "node:path";
import express, { type NextFunction, type Request, type Response } from "express";
import type { ViteDevServer } from "vite";
import { registerLocalAuthRoutes, setupLocalAuth } from "./local-auth.js";
import { MILJØ } from "./env.js";
import { hentSaksbehandler } from "./utils/token.js";
import { lagApiProxy } from "./api-proxy.js";
import { lagReactRouterDevHandler, lagViteDevServer } from "./vite-dev.js";
import { exposeMetrics, recordHttpMetrics } from "./metrics.js";
import { setServerTimingHeader } from "./server-timing.js";
import { structuredLog } from "./structured-log.js";
import type { Oppsett } from "./types.js";

const PORT_NUMMER = process.env.PORT;
const KLIENT_MAPPE = path.resolve("build/client");
const GRUNN_OG_HJELP_BEHANDLING_URL_DEV = "http://grunn-og-hjelpestonad";
const GRUNN_OG_HJELP_BEHANDLING_AUDIENCE_DEV =
  "api://dev-gcp.grunn-og-hjelp.grunn-og-hjelpestonad/.default";

const hentBackendUrl = (): string => {
  if (MILJØ.env === "lokalt") {
    return "http://localhost:8082";
  }
  return "http://grunn-og-hjelpestonad";
};

const BACKEND_URL = hentBackendUrl();

if (!BACKEND_URL) {
  throw new Error("BACKEND_URL miljøvariabel må være satt");
}

structuredLog("info", "backend_configured", {
  environment: MILJØ.env,
  backend_host: new URL(BACKEND_URL).host,
});

const erLokaltMiljø = MILJØ.erLokalt;

const viteDevServer: ViteDevServer | undefined = erLokaltMiljø
  ? await lagViteDevServer()
  : undefined;

const app = express();

app.use((_req: Request, res: Response, next: NextFunction) => {
  setServerTimingHeader(res);
  next();
});
app.use(recordHttpMetrics);

if (erLokaltMiljø) {
  setupLocalAuth(app, PORT_NUMMER);
}

app.get("/isAlive", (_req: Request, res: Response) => {
  res.status(200).send("OK");
});

app.get("/isReady", (_req: Request, res: Response) => {
  res.status(200).send("OK");
});

app.get("/metrics", exposeMetrics);

app.use(express.json());

app.use(
  "/api/etterlatte-behandling",
  lagApiProxy(
    GRUNN_OG_HJELP_BEHANDLING_URL_DEV,
    erLokaltMiljø,
    erLokaltMiljø ? undefined : GRUNN_OG_HJELP_BEHANDLING_AUDIENCE_DEV
  )
);
app.use("/api", lagApiProxy(BACKEND_URL, erLokaltMiljø));

if (erLokaltMiljø) {
  registerLocalAuthRoutes(app);
} else {
  app.get("/oauth2/logout", (_req: Request, res: Response) => {
    res.redirect("/oauth2/logout");
  });
}

app.get("/oppsett", (req: Request, res: Response) => {
  const oppsett: Oppsett = {
    saksbehandler: hentSaksbehandler(req, erLokaltMiljø) ?? null,
    env: MILJØ.env,
    telemetryCollectorUrl: process.env.NAIS_FRONTEND_TELEMETRY_COLLECTOR_URL,
  };
  res.setHeader("Cache-Control", "no-store");
  res.json(oppsett);
});

if (viteDevServer) {
  app.use(viteDevServer.middlewares);
  app.use(await lagReactRouterDevHandler(viteDevServer));
} else {
  app.use(
    "/assets",
    express.static(path.join(KLIENT_MAPPE, "assets"), {
      immutable: true,
      maxAge: "1y",
      fallthrough: false,
    })
  );
  app.use(express.static(KLIENT_MAPPE, { maxAge: "1h", index: false }));

  // index.html peker på filer med hash i navnet, så den må ikke caches på tvers av deployer.
  app.get("/{*sti}", (_req: Request, res: Response) => {
    res.setHeader("Cache-Control", "no-cache");
    res.sendFile(path.join(KLIENT_MAPPE, "index.html"));
  });
}

app.listen(PORT_NUMMER, () => {
  if (!PORT_NUMMER) {
    throw new Error("PORT miljøvariabel må være satt. Har du kjørt scriptet for å hente secrets?");
  }

  structuredLog("info", "server_started", {
    port: Number(PORT_NUMMER),
  });
});
