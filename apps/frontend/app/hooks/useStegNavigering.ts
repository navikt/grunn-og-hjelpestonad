import { useNavigate } from "react-router";
import { useBehandlingSteg } from "~/hooks/useBehandlingSteg";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";

export function useStegNavigering(stegPath: StegPath) {
  const navigate = useNavigate();
  const { finnNesteSteg, finnForrigeSteg } = useBehandlingSteg();

  const nesteSteg = finnNesteSteg(stegPath);
  const forrigeSteg = finnForrigeSteg(stegPath);

  const navigerTilNeste = () => {
    if (nesteSteg) {
      navigate(`../${nesteSteg.path}`, { relative: "path" });
    }
  };

  const navigerTilForrige = () => {
    if (forrigeSteg) {
      navigate(`../${forrigeSteg.path}`, { relative: "path" });
    }
  };

  return {
    navigerTilNeste,
    navigerTilForrige,
    harNesteSteg: !!nesteSteg,
    harForrigeSteg: !!forrigeSteg,
  };
}
