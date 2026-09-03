import type { Request, Response, NextFunction } from "express";
import { MILJØ } from "./env.js";
import type { Saksbehandler } from "./types.js";

const PUBLIC_PATHS = [
  "/oauth2/login",
  "/oauth2/callback",
  "/oauth2/logout",
  "/isAlive",
  "/isReady",
];

export const kreverAuthMiddleware = (req: Request, res: Response, next: NextFunction): void => {
  const erPublicPath = PUBLIC_PATHS.some((path) => req.path.startsWith(path));
  const erAsset = req.path.startsWith("/assets") || req.path.includes(".");

  if (MILJØ.env === "lokalt" && !erPublicPath && !erAsset && !req.session.user) {
    req.session.user = mockSaksbehandler;
  }

  if (!erPublicPath && !erAsset && !req.session.user) {
    res.redirect("/oauth2/login");
    return;
  }

  next();
};

export const mockSaksbehandler: Saksbehandler = {
  navn: "Saksbehandler Saksbehandleresen",
  epost: "saksbehandler.test@nav.no",
  navIdent: "Z123456",
  accessToken: process.env.ACCESS_TOKEN_LOKALT,
};
