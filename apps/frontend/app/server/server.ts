import express, { type NextFunction, type Request, type RequestHandler, type Response, } from "express";
import type { ViteDevServer } from "vite";
import { registerLocalAuthRoutes, setupLocalAuth } from "./local-auth.js";
import { MILJØ } from "./env.js";
import { hentSaksbehandlerFraHeaders } from "./utils/token.js";
import { lagApiProxy } from "./api-proxy.js";
import { lagViteDevServer } from "./vite-dev.js";

const PORT_NUMMER = process.env.PORT;
const GRUNN_OG_HJELP_BEHANDLING_URL_DEV = "http://grunn-og-hjelpestonad-backend";
const GRUNN_OG_HJELP_BEHANDLING_AUDIENCE_DEV =
  "api://dev-gcp.grunn-og-hjelp.grunn-og-hjelpestonad-backend/.default";

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

type ReactRouterServerModule = {
  app: RequestHandler;
};

const getReactRouterApp = async (): Promise<RequestHandler> => {
  if (viteDevServer) {
    const serverModule = (await viteDevServer.ssrLoadModule(
      "./server/app.ts"
    )) as ReactRouterServerModule;
    return serverModule.app;
  }

  // Vite generates this module during the frontend build.
  // @ts-expect-error The generated server module is unavailable during TypeScript compilation.
  const serverModule = (await import("../build/server/index.js")) as ReactRouterServerModule;
  return serverModule.app;
};

const handleReactRouterRequest = async (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  res.locals.saksbehandler = erLokaltMiljø
      ? (req.session?.localAuthUser || undefined)
      : hentSaksbehandlerFraHeaders(req);

  try {
    const reactRouterApp = await getReactRouterApp();
    await reactRouterApp(req, res, next);
  } catch (error) {
    if (viteDevServer && error instanceof Error) {
      viteDevServer.ssrFixStacktrace(error);
    }
    next(error);
  }
};

app.use(handleReactRouterRequest);

app.listen(PORT_NUMMER, () => {
  if (!PORT_NUMMER) {
    throw new Error("PORT miljøvariabel må være satt. Har du kjørt scriptet for å hente secrets?");
  }

  console.log(`\nhttp://localhost:${PORT_NUMMER}/`);
});
