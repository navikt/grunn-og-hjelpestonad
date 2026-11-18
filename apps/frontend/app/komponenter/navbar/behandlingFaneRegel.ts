import type { BehandlingResponse } from "~/api/generated/types.gen";

export type NåværendeSteg = BehandlingResponse["nåværendeSteg"];
export type BehandlingForFaner = Pick<BehandlingResponse, "status" | "nåværendeSteg">;

// Backend-stegene i rekkefølge. Et steg er nådd når det står på samme plass som nåværendeSteg eller før.
const NÅVÆRENDE_STEG_REKKEFØLGE: readonly NåværendeSteg[] = [
  "ÅRSAK",
  "INNGANGSVILKÅR",
  "SATS_OG_TRYGDETID",
  "VEDTAK_OG_BREV",
];

// Visningsnavnene må samsvare med backend, fordi de vises i meldingen «Fullfør «…» før du går videre.».
const VISNINGSNAVN_FOR_NÅVÆRENDE_STEG: Record<NåværendeSteg, string> = {
  ÅRSAK: "Årsak behandling",
  INNGANGSVILKÅR: "Vilkår",
  SATS_OG_TRYGDETID: "Sats og trygdetid",
  VEDTAK_OG_BREV: "Simulering og brev",
};

const REDIGERBARE_STATUSER: readonly BehandlingResponse["status"][] = ["OPPRETTET", "UTREDES"];

function erBehandlingRedigerbar(status: BehandlingResponse["status"]): boolean {
  return REDIGERBARE_STATUSER.includes(status);
}

// Uten behandling (ikke lastet ennå) brukes ÅRSAK, fordi det er første steg.
export function visningsnavnForNåværendeSteg(nåværendeSteg: NåværendeSteg | undefined): string {
  return VISNINGSNAVN_FOR_NÅVÆRENDE_STEG[nåværendeSteg ?? "ÅRSAK"];
}

// Før behandlingen er lastet er bare årsak åpen. Når den er lastet og ikke redigerbar, er alle faner åpne.
// Ellers er en fane låst når steget den hører til kommer etter nåværendeSteg.
export function erFaneLåst(
  faneSteg: NåværendeSteg,
  behandling: BehandlingForFaner | undefined
): boolean {
  if (!behandling) return faneSteg !== "ÅRSAK";
  if (!erBehandlingRedigerbar(behandling.status)) return false;

  return indeksIRekkefølge(faneSteg) > indeksIRekkefølge(behandling.nåværendeSteg);
}

function indeksIRekkefølge(steg: NåværendeSteg): number {
  return NÅVÆRENDE_STEG_REKKEFØLGE.indexOf(steg);
}
