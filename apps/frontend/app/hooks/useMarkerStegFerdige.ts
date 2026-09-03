import { useEffect } from "react";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import type { Steg } from "~/komponenter/navbar/BehandlingFaner";

export function useMarkerStegFerdige(steg: Steg, erFerdig: boolean = true) {
  const { markerStegSomFerdig, ferdigeSteg } = useBehandlingContext();

  useEffect(() => {
    if (erFerdig && !ferdigeSteg.includes(steg)) {
      markerStegSomFerdig(steg);
    }
  }, [erFerdig, steg, markerStegSomFerdig, ferdigeSteg]);
}
