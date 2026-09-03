import type { Request, Response, NextFunction } from "express";
import type { AuthConfig, Saksbehandler } from "./types.js";
import { MILJØ } from "./env.js";
import { mockSaksbehandler } from "./auth-middleware.js";

const TENANT = "trygdeetaten.no";
const AUTHORIZATION_ENDPOINT = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/authorize`;
const TOKEN_ENDPOINT = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/token`;

let authConfig: AuthConfig | null = null;

export function initializeAuth(config: AuthConfig): void {
  authConfig = config;
}

function generateRandomString(length: number = 32): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function generateCodeVerifier(): string {
  return generateRandomString(128);
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest("SHA-256", data);
  return Buffer.from(hash)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

export async function handleLogin(req: Request, res: Response): Promise<void> {
  if (MILJØ.env === "lokalt") {
    req.session.user = mockSaksbehandler;
    res.redirect("/");
    return;
  }

  if (!authConfig) {
    res.status(500).send("Auth not configured");
    return;
  }

  const state = generateRandomString();
  const nonce = generateRandomString();
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);

  req.session.state = state;
  req.session.nonce = nonce;
  req.session.codeVerifier = codeVerifier;

  const backendScope = process.env.GJENLEVENDE_BS_SAK_SCOPE;
  if (!backendScope) {
    res.status(500).send("GJENLEVENDE_BS_SAK_SCOPE miljøvariabel må være satt");
    return;
  }

  const params = new URLSearchParams({
    client_id: authConfig.clientId,
    response_type: "code",
    redirect_uri: authConfig.redirectUri,
    response_mode: "query",
    scope: `openid profile email ${backendScope}`,
    state,
    nonce,
    code_challenge: codeChallenge,
    code_challenge_method: "S256",
  });

  const authUrl = `${AUTHORIZATION_ENDPOINT}?${params.toString()}`;
  res.redirect(authUrl);
}

export async function handleCallback(req: Request, res: Response): Promise<void> {
  if (!authConfig) {
    res.status(500).send("Auth not configured");
    return;
  }

  const { code, state } = req.query;

  if (state !== req.session.state) {
    res.status(400).send("Invalid state parameter");
    return;
  }

  if (!code || typeof code !== "string") {
    res.status(400).send("No authorization code provided");
    return;
  }

  try {
    const tokenParams = new URLSearchParams({
      grant_type: "authorization_code",
      client_id: authConfig.clientId,
      client_secret: authConfig.clientSecret,
      code,
      redirect_uri: authConfig.redirectUri,
      code_verifier: req.session.codeVerifier || "",
    });

    const tokenResponse = await fetch(TOKEN_ENDPOINT, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: tokenParams.toString(),
    });

    if (!tokenResponse.ok) {
      const errorText = await tokenResponse.text();
      console.error("Token exchange failed:", errorText);
      res.status(500).send("Failed to exchange code for tokens");
      return;
    }

    const tokens = await tokenResponse.json();

    const idTokenPayload = JSON.parse(
      Buffer.from(tokens.id_token.split(".")[1], "base64").toString()
    );

    console.log("idTokenPayload", idTokenPayload);

    req.session.user = {
      navn: idTokenPayload.name,
      epost: idTokenPayload.email || idTokenPayload.upn,
      oid: idTokenPayload.oid,
      navIdent: idTokenPayload.NAVident,
      accessToken: tokens.access_token,
    } as Saksbehandler;

    delete req.session.state;
    delete req.session.nonce;
    delete req.session.codeVerifier;

    res.redirect("/");
  } catch (error) {
    console.error("Authentication callback error:", error);
    res.status(500).send("Authentication failed");
  }
}

export function handleLogout(req: Request, res: Response): void {
  if (MILJØ.env === "lokalt") {
    req.session.user = undefined;
    res.redirect("/");
    return;
  }

  req.session.destroy((err) => {
    if (err) {
      console.error("Logout error:", err);
    }
    res.redirect("/");
  });
}

export function requireAuth(req: Request, res: Response, next: NextFunction): void {
  if (!req.session.user) {
    res.redirect("/oauth2/login");
    return;
  }
  next();
}
