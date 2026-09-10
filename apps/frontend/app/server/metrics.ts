import type { NextFunction, Request, Response } from "express";
import { Counter, Histogram, Registry, collectDefaultMetrics } from "prom-client";

const registry = new Registry();

collectDefaultMetrics({
  prefix: "frontend_",
  register: registry,
});

const httpRequests = new Counter({
  name: "frontend_http_requests_total",
  help: "Total number of HTTP requests handled by the frontend BFF.",
  labelNames: ["method", "route", "status"] as const,
  registers: [registry],
});

const httpRequestDuration = new Histogram({
  name: "frontend_http_request_duration_seconds",
  help: "HTTP request duration for the frontend BFF.",
  labelNames: ["method", "route"] as const,
  registers: [registry],
});

const getMetricRoute = (request: Request): string => {
  const path = request.path;

  if (path === "/isAlive") {
    return "health_liveness";
  }

  if (path === "/isReady") {
    return "health_readiness";
  }

  if (path.startsWith("/api/")) {
    return "api";
  }

  if (path.startsWith("/assets/")) {
    return "assets";
  }

  if (path.startsWith("/oauth2/")) {
    return "authentication";
  }

  return "frontend";
};

export function recordHttpMetrics(request: Request, response: Response, next: NextFunction): void {
  if (request.path === "/metrics") {
    next();
    return;
  }

  const stopTimer = httpRequestDuration.startTimer();

  response.on("finish", () => {
    const route = getMetricRoute(request);
    const labels = {
      method: request.method,
      route,
    };

    httpRequests.inc({
      ...labels,
      status: String(response.statusCode),
    });
    stopTimer(labels);
  });

  next();
}

export async function exposeMetrics(_request: Request, response: Response): Promise<void> {
  response.setHeader("Content-Type", registry.contentType);
  response.end(await registry.metrics());
}
