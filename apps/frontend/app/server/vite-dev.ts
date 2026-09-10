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
