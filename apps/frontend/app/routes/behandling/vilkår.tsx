import React, { useState } from "react";
import { VStack } from "@navikt/ds-react";
import { useMarkerStegFerdige } from "~/hooks/useMarkerStegFerdige";
import { VilkårInnhold } from "~/komponenter/behandling/vilkår/VilkårInnhold";
import type { Route } from "./+types/vilkår";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useStegNavigering } from "~/hooks/useStegNavigering";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Vilkår" }];
}

const STEG_PATH: StegPath = "vilkar";

export default function Vilkår() {
  const [erVilkårUtfylt, settErVilkårUtfylt] = useState<boolean>(false);
  const { navigerTilNeste } = useStegNavigering(STEG_PATH);

  useMarkerStegFerdige("Vilkår", erVilkårUtfylt === true);
  const harFyltUtAlt = erVilkårUtfylt;

  const handleNesteKlikk = () => {
    if (harFyltUtAlt) {
      settErVilkårUtfylt(true);
      navigerTilNeste();
    }
  };

  return (
    <VStack gap={"space-24"}>
      <VilkårInnhold settErVilkårUtfylt={settErVilkårUtfylt} />

      <StegNavigering
        stegPath={STEG_PATH}
        nesteDisabled={!harFyltUtAlt}
        onNeste={handleNesteKlikk}
      />
    </VStack>
  );
}
