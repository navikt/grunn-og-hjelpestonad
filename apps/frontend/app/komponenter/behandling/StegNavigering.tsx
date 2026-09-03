import React from "react";
import { Button, HStack } from "@navikt/ds-react";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";

interface StegNavigeringProps {
  stegPath: StegPath;
  nesteDisabled?: boolean;
  onNeste?: () => void;
}

export const StegNavigering: React.FC<StegNavigeringProps> = ({
  stegPath,
  nesteDisabled,
  onNeste,
}) => {
  const { navigerTilNeste, navigerTilForrige, harNesteSteg, harForrigeSteg } =
    useStegNavigering(stegPath);

  const handleNeste = onNeste ?? navigerTilNeste;

  const justify = harForrigeSteg && harNesteSteg ? "space-between" : harNesteSteg ? "end" : "start";

  return (
    <HStack justify={justify}>
      {harForrigeSteg && (
        <Button variant="secondary" onClick={navigerTilForrige}>
          Tilbake
        </Button>
      )}
      {harNesteSteg && (
        <Button onClick={handleNeste} disabled={nesteDisabled}>
          Neste
        </Button>
      )}
    </HStack>
  );
};
