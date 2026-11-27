import type { BehandlingResponse } from "~/api/generated/types.gen";

export type GjeldendeSteg = BehandlingResponse["gjeldendeSteg"];
export type BehandlingForFaner = Pick<BehandlingResponse, "status" | "gjeldendeSteg">;

// Backend-stegene i rekkefølge. Et steg er nådd når det står på samme plass som gjeldendeSteg eller før.
const GJELDENDE_STEG_REKKEFØLGE: readonly GjeldendeSteg[] = [
  "ÅRSAK",
  "INNGANGSVILKÅR",
  "SATS_OG_TRYGDETID",
  "VEDTAK_OG_BREV",
];

// Visningsnavnene må samsvare med backend, fordi de vises i meldingen «Fullfør «…» før du går videre.».
const VISNINGSNAVN_FOR_GJELDENDE_STEG: Record<GjeldendeSteg, string> = {
  ÅRSAK: "Årsak behandling",
  INNGANGSVILKÅR: "Inngangsvilkår",
  SATS_OG_TRYGDETID: "Sats og trygdetid",
  VEDTAK_OG_BREV: "Simulering og brev",
};

const REDIGERBARE_STATUSER: readonly BehandlingResponse["status"][] = ["OPPRETTET", "UTREDES"];

function erBehandlingRedigerbar(status: BehandlingResponse["status"]): boolean {
  return REDIGERBARE_STATUSER.includes(status);
}

// Uten behandling (ikke lastet ennå) brukes ÅRSAK, fordi det er første steg.
export function visningsnavnForGjeldendeSteg(gjeldendeSteg: GjeldendeSteg | undefined): string {
  return VISNINGSNAVN_FOR_GJELDENDE_STEG[gjeldendeSteg ?? "ÅRSAK"];
}

// Før behandlingen er lastet er bare årsak åpen. Når den er lastet og ikke redigerbar, er alle faner åpne.
// Ellers er en fane låst når steget den hører til kommer etter gjeldendeSteg.
export function erFaneLåst(
  faneSteg: GjeldendeSteg,
  behandling: BehandlingForFaner | undefined
): boolean {
  if (!behandling) return faneSteg !== "ÅRSAK";
  if (!erBehandlingRedigerbar(behandling.status)) return false;

  return indeksIRekkefølge(faneSteg) > indeksIRekkefølge(behandling.gjeldendeSteg);
}

function indeksIRekkefølge(steg: GjeldendeSteg): number {
  return GJELDENDE_STEG_REKKEFØLGE.indexOf(steg);
}
