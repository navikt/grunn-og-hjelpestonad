export const ToggleNavn = {
  TestToggle: "grunn-og-hjelp_frontend__test_setup",
} as const;

type ToggleNøkkel = keyof typeof ToggleNavn;

type ToggelVerdi = (typeof ToggleNavn)[ToggleNøkkel];

export type Toggles = Partial<Record<ToggelVerdi, boolean>>;
