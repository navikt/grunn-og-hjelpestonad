import session from "express-session";
import type { SessionOptions } from "express-session";

const ÅTTE_TIMER = 1000 * 60 * 60 * 8;

export function lagSessionMiddleware(): SessionOptions {
  return {
    secret: process.env.SESSION_SECRET || "fallback-secret-key",
    resave: false,
    saveUninitialized: false,
    cookie: {
      secure: false,
      httpOnly: true,
      maxAge: ÅTTE_TIMER,
    },
  };
}

export { session };
