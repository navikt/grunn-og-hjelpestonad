import type { RequestHandler } from "express";
import type { ServerBuild } from "react-router";
import type { ViteDevServer } from "vite";
import { structuredLog } from "./structured-log.js";

export async function lagViteDevServer(): Promise<ViteDevServer | undefined> {
  try {
    const vite = await import("vite");
    return vite.createServer({
      server: {
        middlewareMode: true,
        host: true,
      },
    });
  } catch (error) {
    structuredLog("error", "vite_dev_server_creation_failed", {
      error_type: error instanceof Error ? error.name : "unknown",
    });
    return undefined;
  }
}

// Vite i middleware-modus serverer ikke HTML selv, så lokalt rendrer React Router SPA-skallet per request.
export async function lagReactRouterDevHandler(vite: ViteDevServer): Promise<RequestHandler> {
  const { createRequestHandler } = await import("@react-router/express");
  return createRequestHandler({
    build: () => vite.ssrLoadModule("virtual:react-router/server-build") as Promise<ServerBuild>,
  });
}
