export const ÅRSAK = {
  SØKNAD: "SØKNAD",
  NYE_OPPLYSNINGER: "NYE_OPPLYSNINGER",
  ANNET: "ANNET",
} as const;

export type ÅrsakType = (typeof ÅRSAK)[keyof typeof ÅRSAK];
