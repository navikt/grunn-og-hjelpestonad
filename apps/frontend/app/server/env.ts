export type AppEnv = "lokalt" | "lokalt-mot-preprod" | "development" | "production";

export interface EnvConfig {
  env: AppEnv;
  erLokalt: boolean;
  erLokaltMotPreprod: boolean;
  erDev: boolean;
  erProduksjon: boolean;
}

function parseAppEnv(): AppEnv {
  const envVar = process.env.ENV?.toLowerCase();

  if (envVar === "lokalt") {
    return "lokalt";
  }

  if (envVar === "lokalt-mot-preprod") {
    return "lokalt-mot-preprod";
  }

  if (envVar === "development" || envVar === "dev") {
    return "development";
  }

  if (envVar === "production") {
    return "production";
  }

  return "production";
}

export function hentEnvConfig(): EnvConfig {
  const env = parseAppEnv();

  return {
    env,
    erLokalt: env === "lokalt",
    erLokaltMotPreprod: env === "lokalt-mot-preprod",
    erDev: env === "development",
    erProduksjon: env === "production",
  };
}

export const MILJØ = hentEnvConfig();
