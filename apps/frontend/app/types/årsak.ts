import type { ÅrsakBehandlingResponse } from "~/api/generated/types.gen";

export const ÅRSAK = {
  SØKNAD: "SØKNAD",
  NYE_OPPLYSNINGER: "NYE_OPPLYSNINGER",
  ANNET: "ANNET",
} as const;

export type ÅrsakType = ÅrsakBehandlingResponse["årsak"];
