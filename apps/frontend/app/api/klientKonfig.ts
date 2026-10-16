import type { CreateClientConfig } from "~/api/generated/client.gen";

/**
 * Kjøretidskonfigurasjon for den genererte API-klienten. De genererte path-ene inneholder
 * allerede `/api`-prefikset fra controllerne, så `baseUrl` må være tom (til forskjell fra
 * `app/api/klientOppsett.ts`, fordi fetch-klienten ikke tar interceptors gjennom denne
 * configen.
 */
export const createClientConfig: CreateClientConfig = (config) => ({
  ...config,
  baseUrl: "",
  headers: {
    "Content-Type": "application/json",
    ...config?.headers,
  },
});
