import { Box, VStack } from "@navikt/ds-react";
import React from "react";
import type { Route } from "./+types/simulering";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { OppdragSimulering } from "~/komponenter/simulering/OppdragSimulering";

export function meta(_args: Route.MetaArgs) {
  return [
    { title: "Simulering" },
    {
      name: "Simulering",
      content: "Simuleringsside",
    },
  ];
}

const STEG_PATH: StegPath = "simulering";

export default function Simulering() {
  const { behandlingId } = useBehandlingContext();
  const { harForrigeSteg, navigerTilNeste } = useStegNavigering(STEG_PATH);

  return (
    <VStack gap={"space-24"}>
      <OppdragSimulering behandlingId={behandlingId} />
      {harForrigeSteg && (
        <Box>
          <StegNavigering stegPath={STEG_PATH} onNeste={navigerTilNeste} />
        </Box>
      )}
    </VStack>
  );
}
