import express, { type NextFunction, type Request, type RequestHandler, type Response, } from "express";
import { SpanStatusCode, trace } from "@opentelemetry/api";
import type { ViteDevServer } from "vite";
import { registerLocalAuthRoutes, setupLocalAuth } from "./local-auth.js";
import { MILJØ } from "./env.js";
import { hentSaksbehandlerFraHeaders } from "./utils/token.js";
import { lagApiProxy } from "./api-proxy.js";
import { lagViteDevServer } from "./vite-dev.js";
import { exposeMetrics, recordHttpMetrics } from "./metrics.js";
import { setServerTimingHeader } from "./server-timing.js";
import { structuredLog } from "./structured-log.js";

const PORT_NUMMER = process.env.PORT;
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
const debugTracer = trace.getTracer("grunn-og-hjelpestonad-frontend-debug");

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

const forhåndslastetReactRouterApp = viteDevServer
    ? undefined
    : await getReactRouterApp();

const handleReactRouterRequest = async (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  await debugTracer.startActiveSpan("frontend.react_router.request", async (requestSpan) => {
    requestSpan.setAttributes({
      "http.request.method": req.method,
      "frontend.request.kind": req.path.endsWith(".data") ? "react-router-data" : "page",
    });

    try {
      await debugTracer.startActiveSpan("frontend.react_router.context", async (contextSpan) => {
        contextSpan.setAttribute("frontend.auth.mode", erLokaltMiljø ? "local" : "header-token");

        try {
          res.locals.saksbehandler = erLokaltMiljø
            ? req.session?.localAuthUser || undefined
            : hentSaksbehandlerFraHeaders(req);
        } finally {
          contextSpan.end();
        }
      });

      await debugTracer.startActiveSpan("frontend.react_router.handle", async (handleSpan) => {
        try {
          const reactRouterApp = await debugTracer.startActiveSpan(
            "frontend.react_router.load_app",
            async (loadSpan) => {
              try {
                return forhåndslastetReactRouterApp ?? (await getReactRouterApp());
              } finally {
                loadSpan.end();
              }
            }
          );
          await reactRouterApp(req, res, next);
        } finally {
          handleSpan.end();
        }
      });
    } catch (error) {
      if (error instanceof Error) {
        requestSpan.recordException(error);
      }
      requestSpan.setStatus({ code: SpanStatusCode.ERROR });

      if (viteDevServer && error instanceof Error) {
        viteDevServer.ssrFixStacktrace(error);
      }
      next(error);
    } finally {
      requestSpan.setAttribute("http.response.status_code", res.statusCode);
      requestSpan.end();
    }
  });
};

app.use(handleReactRouterRequest);

app.listen(PORT_NUMMER, () => {
  if (!PORT_NUMMER) {
    throw new Error("PORT miljøvariabel må være satt. Har du kjørt scriptet for å hente secrets?");
  }

  structuredLog("info", "server_started", {
    port: Number(PORT_NUMMER),
  });
});
