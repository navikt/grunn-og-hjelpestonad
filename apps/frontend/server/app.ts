import { createRequestHandler } from "@react-router/express";
import { SpanStatusCode, trace } from "@opentelemetry/api";
import express from "express";
import { RouterContextProvider } from "react-router";
import { envContext, saksbehandlerContext } from "../app/context";
import { MILJØ } from "../app/server/env";
import type { Saksbehandler } from "../app/server/types";

export const app = express();

const debugTracer = trace.getTracer("grunn-og-hjelpestonad-frontend-debug");

const build = () =>
  debugTracer.startActiveSpan(
    "frontend.react_router.build_import",
    async (span) => {
      try {
        return await import("virtual:react-router/server-build");
      } catch (error) {
        if (error instanceof Error) {
          span.recordException(error);
        }
        span.setStatus({ code: SpanStatusCode.ERROR });
        throw error;
      } finally {
        span.end();
      }
    }
  );

const reactRouterHandler = createRequestHandler({
  build,
  getLoadContext: (_request, response) => {
    const context = new RouterContextProvider();
    const locals: { saksbehandler?: Saksbehandler } = response.locals;
    context.set(saksbehandlerContext, locals.saksbehandler ?? null);
    context.set(envContext, MILJØ.env);
    return context;
  },
});

app.use(async (req, res, next) =>
  debugTracer.startActiveSpan(
    "frontend.react_router.adapter",
    async (span) => {
      span.setAttribute("http.request.method", req.method);
      span.setAttribute(
        "frontend.request.kind",
        req.path.endsWith(".data") ? "react-router-data" : "page"
      );

      try {
        await reactRouterHandler(req, res, next);
      } catch (error) {
        if (error instanceof Error) {
          span.recordException(error);
        }
        span.setStatus({ code: SpanStatusCode.ERROR });
        throw error;
      } finally {
        span.end();
      }
    }
  )
);
