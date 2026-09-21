import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: process.env.OPENAPI_URL ?? "http://localhost:8082/v3/api-docs",
  output: {
    path: "app/api/generated",
    entryFile: false,
  },
  plugins: [
    {
      name: "@hey-api/typescript",
      comments: false,
    },
    {
      name: "@hey-api/client-fetch",
      runtimeConfigPath: "./app/api/klientKonfig",
    },
    {
      name: "@hey-api/sdk",
      validator: true,
    },
    {
      name: "zod",
      compatibilityVersion: 4,
    },
  ],
});
