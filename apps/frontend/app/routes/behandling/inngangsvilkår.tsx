import React from "react";
import { useNavigate } from "react-router";
import { LocalAlert, VStack } from "@navikt/ds-react";
import { VilkårProvider, useVilkårContext } from "~/komponenter/behandling/vilkår/VilkårContext";
import { VilkårInnhold } from "~/komponenter/behandling/vilkår/VilkårInnhold";
import { useFullførInngangsvilkår } from "~/komponenter/behandling/vilkår/felles/useFullførInngangsvilkår";
import { FullførInngangsvilkårFeilvisning } from "~/komponenter/behandling/vilkår/FullførInngangsvilkårFeilvisning";
import type { Route } from "./+types/inngangsvilkår";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useStegNavigering } from "~/hooks/useStegNavigering";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Inngangsvilkår" }];
}

const STEG_PATH: StegPath = "vilkar";

export default function Inngangsvilkår() {
  return (
    <VilkårProvider>
      <InngangsvilkårSide />
    </VilkårProvider>
  );
}

function InngangsvilkårSide() {
  const navigate = useNavigate();
  const { behandlingId, hentBehandlingPåNytt } = useBehandlingContext();
  const { status: vilkårStatus } = useVilkårContext();
  const { fullfør, laster, feil: fullføringsfeil } = useFullførInngangsvilkår(behandlingId);

  const { navigerTilNeste } = useStegNavigering(STEG_PATH);

  const håndterNeste = () =>
    fullfør(async (resultat) => {
      // Fullføringen kan flytte gjeldende steg, så behandlingen hentes på nytt før navigering.
      await hentBehandlingPåNytt();

      switch (resultat.nesteSteg) {
        case "SATS_OG_TRYGDETID":
          navigerTilNeste();
          return;
        case "VEDTAK_OG_BREV":
          navigate("../simulering", { relative: "path" });
          return;
      }
    });

  return (
    <VStack gap={"space-24"}>
      {vilkårStatus.feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Content>{vilkårStatus.feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      <VilkårInnhold />

      {fullføringsfeil && <FullførInngangsvilkårFeilvisning feil={fullføringsfeil} />}

      <StegNavigering stegPath={STEG_PATH} onNeste={håndterNeste} nesteLaster={laster} />
    </VStack>
  );
}
