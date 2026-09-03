import { useContext } from "react";
import { BehandlingContext } from "~/contexts/BehandlingContext";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";

export function useBehandlingSteg() {
  const { stegListe, ferdigeSteg } = useContext(BehandlingContext);

  if (!stegListe) {
    throw new Error("useBehandlingSteg må brukes innenfor BehandlingContext med behandlingSteg");
  }

  const finnSteg = (nåværendeStegPath: StegPath, indexRetning: number) => {
    const nåværendeIndex = stegListe.findIndex((steg) => steg.path === nåværendeStegPath);
    const targetIndex = nåværendeIndex + indexRetning;

    if (nåværendeIndex === -1 || targetIndex < 0 || targetIndex >= stegListe.length) {
      return null;
    }

    return stegListe[targetIndex];
  };

  const finnNesteSteg = (path: StegPath) => finnSteg(path, 1);
  const finnForrigeSteg = (path: StegPath) => finnSteg(path, -1);

  return {
    stegListe,
    ferdigeSteg,
    finnNesteSteg,
    finnForrigeSteg,
  };
}
