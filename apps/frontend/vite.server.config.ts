import { defineConfig } from "vite";

// Bundler Express-serveren med alle avhengigheter, så Docker-imaget ikke trenger node_modules.
export default defineConfig({
  ssr: {
    noExternal: true,
    // Brukes bare lokalt (vite-dev.ts), og da kjøres server.ts via tsx.
    external: ["vite", "@react-router/express"],
  },
  build: {
    ssr: "app/server/server.ts",
    outDir: "dist",
    emptyOutDir: true,
  },
});
