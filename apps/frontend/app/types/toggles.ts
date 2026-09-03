export const ToggleNavn = {
  TestToggle: "gjenlevende_frontend__test_setup",
} as const;

type ToggleNøkkel = keyof typeof ToggleNavn;

type ToggelVerdi = (typeof ToggleNavn)[ToggleNøkkel];

export type Toggles = Partial<Record<ToggelVerdi, boolean>>;
