import type { ViteDevServer } from "vite";

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
    console.error("Kunne ikke opprette Vite dev server:", error);
    return undefined;
  }
}
