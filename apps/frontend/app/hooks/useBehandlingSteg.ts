import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";

export function useBehandlingSteg() {
  const { stegListe } = useBehandlingContext();

  if (!stegListe) {
    throw new Error("useBehandlingSteg må brukes innenfor BehandlingContext med behandlingSteg");
  }

  const finnSteg = (gjeldendeStegPath: StegPath, indexRetning: number) => {
    const gjeldendeIndex = stegListe.findIndex((steg) => steg.path === gjeldendeStegPath);
    const targetIndex = gjeldendeIndex + indexRetning;

    if (gjeldendeIndex === -1 || targetIndex < 0 || targetIndex >= stegListe.length) {
      return null;
    }

    return stegListe[targetIndex];
  };

  const finnNesteSteg = (path: StegPath) => finnSteg(path, 1);
  const finnForrigeSteg = (path: StegPath) => finnSteg(path, -1);

  return {
    stegListe,
    finnNesteSteg,
    finnForrigeSteg,
  };
}
