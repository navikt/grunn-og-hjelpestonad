import { createRequestHandler } from "@react-router/express";
import express from "express";
import { RouterContextProvider } from "react-router";
import { envContext, saksbehandlerContext } from "../app/context";
import { MILJØ } from "../app/server/env";
import type { Saksbehandler } from "../app/server/types";

export const app = express();

const build = () => import("virtual:react-router/server-build");

app.use(
  createRequestHandler({
    build,
    getLoadContext: (_request, response) => {
      const context = new RouterContextProvider();
      const locals: { saksbehandler?: Saksbehandler } = response.locals;
      context.set(saksbehandlerContext, locals.saksbehandler ?? null);
      context.set(envContext, MILJØ.env);
      return context;
    },
  })
);
