import express, { type Request, type Response } from "express";
import { createRequestListener } from "@react-router/node";
import type { ServerBuild } from "react-router";
import type { ViteDevServer } from "vite";
import cookieParser from "cookie-parser";
import { AsyncLocalStorage } from "node:async_hooks";
import "dotenv/config";
import { initializeAuth, handleLogin, handleCallback, handleLogout } from "./auth.js";
import type { Saksbehandler } from "./types.js";
import { MILJØ } from "./env.js";
import { hentSaksbehandlerFraHeaders } from "./utils/token.js";
import { lagApiProxy } from "./api-proxy.js";
import { kreverAuthMiddleware } from "./auth-middleware.js";
import { session, lagSessionMiddleware } from "./session.js";
import { lagViteDevServer } from "./vite-dev.js";

const PORT_NUMMER = process.env.PORT;
const ETTERLATTE_BEHANDLING_URL_DEV = "http://etterlatte-behandling";
const ETTERLATTE_BEHANDLING_AUDIENCE_DEV =
  "api://dev-gcp.etterlatte.etterlatte-behandling/.default";

const hentBackendUrl = (): string => {
  if (MILJØ.env === "lokalt") {
    return "http://localhost:8082";
  }
  if (MILJØ.erLokaltMotPreprod) {
    return "https://gjenlevende-bs-sak.intern.dev.nav.no";
  }
  return "http://gjenlevende-bs-sak";
};

const BACKEND_URL = hentBackendUrl();

if (!BACKEND_URL) {
  throw new Error("BACKEND_URL miljøvariabel må være satt");
}

console.log(`Backend URL: ${BACKEND_URL} (ENV: ${MILJØ.env})`);

declare module "express-session" {
  interface SessionData {
    user?: Saksbehandler;
    state?: string;
    nonce?: string;
    codeVerifier?: string;
  }
}

const erLokaltMiljø = MILJØ.erLokalt || MILJØ.erLokaltMotPreprod;
const skalBrukeViteDevServer = MILJØ.erLokalt || MILJØ.erLokaltMotPreprod;

const viteDevServer: ViteDevServer | undefined = skalBrukeViteDevServer
  ? await lagViteDevServer()
  : undefined;

const app = express();
const saksbehandlerStorage = new AsyncLocalStorage<Saksbehandler | undefined>();

if (erLokaltMiljø) {
  app.use(cookieParser());
  app.use(session(lagSessionMiddleware()));

  if (MILJØ.erLokaltMotPreprod && process.env.CLIENT_ID && process.env.CLIENT_SECRET) {
    initializeAuth({
      clientId: process.env.CLIENT_ID,
      clientSecret: process.env.CLIENT_SECRET,
      redirectUri: `http://localhost:${PORT_NUMMER}/oauth2/callback`,
    });
  }
}

function hentSaksbehandlerInfoFraHeaders(req: Request): Saksbehandler | undefined {
  return hentSaksbehandlerFraHeaders(req);
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
  app.get("/oauth2/login", handleLogin);
  app.get("/oauth2/callback", handleCallback);
  app.get("/oauth2/logout", handleLogout);

  app.use(kreverAuthMiddleware);
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
    ? req.session?.user || undefined
    : hentSaksbehandlerInfoFraHeaders(req);

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
