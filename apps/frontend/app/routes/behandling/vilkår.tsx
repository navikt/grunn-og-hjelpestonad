import React, { useEffect, useRef } from "react";
import { ErrorSummary, LocalAlert, VStack } from "@navikt/ds-react";
import { VilkårProvider, useVilkårContext } from "~/komponenter/behandling/vilkår/VilkårContext";
import { VilkårInnhold } from "~/komponenter/behandling/vilkår/VilkårInnhold";
import { useFullførVilkårSteg } from "~/komponenter/behandling/vilkår/felles/useFullførVilkårSteg";
import type { Route } from "./+types/vilkår";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useStegNavigering } from "~/hooks/useStegNavigering";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Vilkår" }];
}

const STEG_PATH: StegPath = "vilkar";

export default function Vilkår() {
  return (
    <VilkårProvider>
      <VilkårSide />
    </VilkårProvider>
  );
}

function VilkårSide() {
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);

  const { behandlingId } = useBehandlingContext();
  const { status: vilkårStatus } = useVilkårContext();
  const { fullfør, laster, valideringsfeil, feilmelding } = useFullførVilkårSteg(behandlingId);

  const { navigerTilNeste } = useStegNavigering(STEG_PATH);

  useEffect(() => {
    if (valideringsfeil.length > 0) {
      feiloppsummeringRef.current?.focus();
    }
  }, [valideringsfeil]);

  const håndterNeste = () => fullfør(navigerTilNeste);

  return (
    <VStack gap={"space-24"}>
      {vilkårStatus.feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Content>{vilkårStatus.feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      <VilkårInnhold />

      {feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      {valideringsfeil.length > 0 && (
        <ErrorSummary
          ref={feiloppsummeringRef}
          heading="Du må vurdere disse vilkårene før du kan gå videre:"
        >
          {valideringsfeil.map((feil) => (
            <ErrorSummary.Item key={feil.nøkkel} href={`#vilkar-${feil.nøkkel}`}>
              {feil.melding}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      <StegNavigering stegPath={STEG_PATH} onNeste={håndterNeste} nesteLaster={laster} />
    </VStack>
  );
}
