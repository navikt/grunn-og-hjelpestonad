import { createContext } from "react-router";
import type { AppEnv } from "./server/env";
import type { Saksbehandler } from "./server/types";

export const saksbehandlerContext = createContext<Saksbehandler | null>(null);
export const envContext = createContext<AppEnv>();
