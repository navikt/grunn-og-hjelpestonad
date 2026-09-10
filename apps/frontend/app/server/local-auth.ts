import cookieParser from "cookie-parser";
import type { Express, NextFunction, Request, Response } from "express";
import session from "express-session";
import type { SessionOptions } from "express-session";
import { MILJØ } from "./env.js";
import { structuredLog } from "./structured-log.js";
import type { Saksbehandler } from "./types.js";

const LOCAL_AUTH_TENANT = "trygdeetaten.no";
const LOCAL_AUTH_DEFAULT_AUTHORIZATION_ENDPOINT =
  `https://login.microsoftonline.com/${LOCAL_AUTH_TENANT}/oauth2/v2.0/authorize`;
const LOCAL_AUTH_DEFAULT_TOKEN_ENDPOINT =
  `https://login.microsoftonline.com/${LOCAL_AUTH_TENANT}/oauth2/v2.0/token`;
const LOCAL_AUTH_SESSION_MAX_AGE = 1000 * 60 * 60 * 8;
const LOCAL_AUTH_PUBLIC_PATHS = [
  "/oauth2/login",
  "/oauth2/callback",
  "/oauth2/logout",
  "/isAlive",
  "/isReady",
];

export interface LocalAuthConfig {
  clientId: string;
  clientSecret: string;
  redirectUri: string;
  authorizationEndpoint?: string;
  tokenEndpoint?: string;
}

declare module "express-session" {
  interface SessionData {
    localAuthUser?: Saksbehandler;
    localAuthState?: string;
    localAuthNonce?: string;
    localAuthCodeVerifier?: string;
  }
}

let localAuthConfig: LocalAuthConfig | null = null;

export function setupLocalAuth(app: Express, port: string | undefined): void {
  if (!port) {
    throw new Error("PORT miljøvariabel må være satt for lokal autentisering");
  }

  app.use(cookieParser());
  app.use(session(createLocalAuthSessionOptions()));

  const localAuthClientId = process.env.CLIENT_ID;
  const localAuthClientSecret = process.env.CLIENT_SECRET;

  if (localAuthClientId && localAuthClientSecret) {
    localAuthConfig = {
      clientId: localAuthClientId,
      clientSecret: localAuthClientSecret,
      redirectUri: `http://localhost:${port}/oauth2/callback`,
      authorizationEndpoint: process.env.OAUTH2_AUTHORIZATION_ENDPOINT,
      tokenEndpoint: process.env.OAUTH2_TOKEN_ENDPOINT,
    };
  }
}

export function registerLocalAuthRoutes(app: Express): void {
  app.get("/oauth2/login", handleLocalLogin);
  app.get("/oauth2/callback", handleLocalAuthCallback);
  app.get("/oauth2/logout", handleLocalLogout);
  app.use(requireLocalAuthentication);
}

function createLocalAuthSessionOptions(): SessionOptions {
  return {
    secret: process.env.SESSION_SECRET || "fallback-secret-key",
    resave: false,
    saveUninitialized: false,
    cookie: {
      secure: false,
      httpOnly: true,
      maxAge: LOCAL_AUTH_SESSION_MAX_AGE,
    },
  };
}

function generateLocalAuthRandomString(length: number = 32): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function generateLocalAuthCodeVerifier(): string {
  return generateLocalAuthRandomString(128);
}

async function generateLocalAuthCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest("SHA-256", data);
  return Buffer.from(hash)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

async function handleLocalLogin(req: Request, res: Response): Promise<void> {
  if (!localAuthConfig) {
    res.status(500).send("Lokal autentisering er ikke konfigurert");
    return;
  }

  const localAuthState = generateLocalAuthRandomString();
  const localAuthNonce = generateLocalAuthRandomString();
  const localAuthCodeVerifier = generateLocalAuthCodeVerifier();
  const localAuthCodeChallenge = await generateLocalAuthCodeChallenge(localAuthCodeVerifier);

  req.session.localAuthState = localAuthState;
  req.session.localAuthNonce = localAuthNonce;
  req.session.localAuthCodeVerifier = localAuthCodeVerifier;

  const localAuthBackendScope = process.env.GRUNN_OG_HJELPESTONAD_SCOPE;
  if (!localAuthBackendScope) {
    res.status(500).send("GRUNN_OG_HJELPESTONAD_SCOPE miljøvariabel må være satt");
    return;
  }

  const localAuthRequestParams = new URLSearchParams({
    client_id: localAuthConfig.clientId,
    response_type: "code",
    redirect_uri: localAuthConfig.redirectUri,
    response_mode: "query",
    scope: `openid profile email ${localAuthBackendScope}`,
    state: localAuthState,
    nonce: localAuthNonce,
    code_challenge: localAuthCodeChallenge,
    code_challenge_method: "S256",
  });

  const localAuthAuthorizationEndpoint =
    localAuthConfig.authorizationEndpoint ?? LOCAL_AUTH_DEFAULT_AUTHORIZATION_ENDPOINT;
  const localAuthUrl = `${localAuthAuthorizationEndpoint}?${localAuthRequestParams.toString()}`;
  res.redirect(localAuthUrl);
}

async function handleLocalAuthCallback(req: Request, res: Response): Promise<void> {
  if (!localAuthConfig) {
    res.status(500).send("Lokal autentisering er ikke konfigurert");
    return;
  }

  const { code, state } = req.query;

  if (state !== req.session.localAuthState) {
    res.status(400).send("Invalid state parameter");
    return;
  }

  if (!code || typeof code !== "string") {
    res.status(400).send("No authorization code provided");
    return;
  }

  try {
    const localAuthTokenParams = new URLSearchParams({
      grant_type: "authorization_code",
      client_id: localAuthConfig.clientId,
      client_secret: localAuthConfig.clientSecret,
      code,
      redirect_uri: localAuthConfig.redirectUri,
      code_verifier: req.session.localAuthCodeVerifier || "",
    });

    const localAuthTokenEndpoint =
      localAuthConfig.tokenEndpoint ?? LOCAL_AUTH_DEFAULT_TOKEN_ENDPOINT;
    const localAuthTokenResponse = await fetch(localAuthTokenEndpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: localAuthTokenParams.toString(),
    });

    if (!localAuthTokenResponse.ok) {
      await localAuthTokenResponse.text();
      structuredLog("error", "local_auth_token_exchange_failed", {
        status: localAuthTokenResponse.status,
      });
      res.status(500).send("Failed to exchange code for tokens");
      return;
    }

    const localAuthTokens = await localAuthTokenResponse.json();
    const localAuthIdTokenPayload = JSON.parse(
      Buffer.from(localAuthTokens.id_token.split(".")[1], "base64").toString()
    );

    req.session.localAuthUser = {
      navn: localAuthIdTokenPayload.name,
      epost: localAuthIdTokenPayload.email || localAuthIdTokenPayload.upn,
      oid: localAuthIdTokenPayload.oid,
      navIdent: localAuthIdTokenPayload.NAVident,
      accessToken: localAuthTokens.access_token,
    } as Saksbehandler;

    delete req.session.localAuthState;
    delete req.session.localAuthNonce;
    delete req.session.localAuthCodeVerifier;

    res.redirect("/");
  } catch (error) {
    structuredLog("error", "local_authentication_failed", {
      error_type: error instanceof Error ? error.name : "unknown",
    });
    res.status(500).send("Authentication failed");
  }
}

function handleLocalLogout(req: Request, res: Response): void {
  if (MILJØ.env === "lokalt") {
    req.session.localAuthUser = undefined;
    res.redirect("/");
    return;
  }

  req.session.destroy((error) => {
    if (error) {
      structuredLog("error", "local_logout_failed", {
        error_type: error.name,
      });
    }
    res.redirect("/");
  });
}

function requireLocalAuthentication(req: Request, res: Response, next: NextFunction): void {
  const isLocalAuthPublicPath = LOCAL_AUTH_PUBLIC_PATHS.some((path) => req.path.startsWith(path));
  const isLocalAuthAsset = req.path.startsWith("/assets") || req.path.includes(".");

  if (!isLocalAuthPublicPath && !isLocalAuthAsset && !req.session.localAuthUser) {
    res.redirect("/oauth2/login");
    return;
  }

  next();
}
