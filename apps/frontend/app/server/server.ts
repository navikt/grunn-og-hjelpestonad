import express, { type Request, type Response } from "express";
import { createRequestListener } from "@react-router/node";
import type { ServerBuild } from "react-router";
import type { ViteDevServer } from "vite";
import { AsyncLocalStorage } from "node:async_hooks";
import "dotenv/config";
import { registerLocalAuthRoutes, setupLocalAuth } from "./local-auth.js";
import type { Saksbehandler } from "./types.js";
import { MILJØ } from "./env.js";
import { hentSaksbehandlerFraHeaders } from "./utils/token.js";
import { lagApiProxy } from "./api-proxy.js";
import { lagViteDevServer } from "./vite-dev.js";

const PORT_NUMMER = process.env.PORT;
const ETTERLATTE_BEHANDLING_URL_DEV = "http://etterlatte-behandling";
const ETTERLATTE_BEHANDLING_AUDIENCE_DEV =
  "api://dev-gcp.etterlatte.etterlatte-behandling/.default";

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

console.log(`Backend URL: ${BACKEND_URL} (ENV: ${MILJØ.env})`);

const erLokaltMiljø = MILJØ.erLokalt;

const viteDevServer: ViteDevServer | undefined = erLokaltMiljø
  ? await lagViteDevServer()
  : undefined;

const app = express();
const saksbehandlerStorage = new AsyncLocalStorage<Saksbehandler | undefined>();

if (erLokaltMiljø) {
  setupLocalAuth(app, PORT_NUMMER);
}

app.get("/isAlive", (_req: Request, res: Response) => {
  res.status(200).send("OK");
});

app.get("/isReady", (_req: Request, res: Response) => {
  res.status(200).send("OK");
});

app.use(express.json());

app.use(
  "/api/etterlatte-behandling",
  lagApiProxy(
    ETTERLATTE_BEHANDLING_URL_DEV,
    erLokaltMiljø,
    erLokaltMiljø ? undefined : ETTERLATTE_BEHANDLING_AUDIENCE_DEV
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

if (viteDevServer) {
  app.use(viteDevServer.middlewares);
} else {
  app.use(
    "/assets",
    express.static("build/client/assets", {
      immutable: true,
      maxAge: "1y",
    })
  );
}

app.use(express.static("build/client", { maxAge: "1h" }));

const getBuild = async (): Promise<ServerBuild> => {
  if (viteDevServer) {
    return viteDevServer.ssrLoadModule("virtual:react-router/server-build") as Promise<ServerBuild>;
  }
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  return import("../build/server/index.js");
};

const requestListener = createRequestListener({
  build: getBuild,
  getLoadContext: () => ({
    saksbehandler: saksbehandlerStorage.getStore() || null,
    env: MILJØ.env,
  }),
});

app.all("*splat", (req, res) => {
  const saksbehandler = erLokaltMiljø
    ? req.session?.localAuthUser || undefined
    : hentSaksbehandlerFraHeaders(req);

  saksbehandlerStorage.run(saksbehandler, () => {
    requestListener(req, res);
  });
});

app.listen(PORT_NUMMER, () => {
  if (!PORT_NUMMER) {
    throw new Error("PORT miljøvariabel må være satt. Har du kjørt scriptet for å hente secrets?");
  }

  console.log(`\nhttp://localhost:${PORT_NUMMER}/`);
});
