import React from "react";
import type { Route } from "./+types/satsOgTrygdetid";
import { Accordion, VStack } from "@navikt/ds-react";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { PerioderMedOppfylteInngangsvilkårTidslinje } from "~/komponenter/behandling/satsOgTrygdetid/PerioderMedOppfylteInngangsvilkårTidslinje";
import { EkstrautgifterVilkår } from "~/komponenter/behandling/satsOgTrygdetid/ekstrautgifter/EkstrautgifterVilkår";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Sats og trygdetid" }];
}

const STEG_PATH: StegPath = "sats-og-trygdetid";

export default function SatsOgTrygdetid() {
  const { behandlingId } = useBehandlingContext();

  return (
    <VStack gap="space-24">
      {behandlingId && <PerioderMedOppfylteInngangsvilkårTidslinje behandlingId={behandlingId} />}

      {behandlingId && (
        <Accordion>
          <EkstrautgifterVilkår behandlingId={behandlingId} />
        </Accordion>
      )}

      <StegNavigering stegPath={STEG_PATH} />
    </VStack>
  );
}
