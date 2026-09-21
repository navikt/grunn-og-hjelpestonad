/**
 * Backend svarer med `FeilResponse { melding, status }` på håndterte feil. Den genererte
 * klienten kaster selve responsbodyen, så vi plukker ut meldingen her.
 */
export function feilmeldingFra(feil: unknown, standard: string): string {
  if (feil && typeof feil === "object" && "melding" in feil) {
    const melding = (feil as { melding?: unknown }).melding;
    if (typeof melding === "string" && melding.trim() !== "") {
      return melding;
    }
  }
  return standard;
}
