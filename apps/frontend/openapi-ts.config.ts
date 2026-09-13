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
  ],
});
