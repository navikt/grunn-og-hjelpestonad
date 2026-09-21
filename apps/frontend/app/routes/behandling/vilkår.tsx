import React, { useEffect, useRef, useState } from "react";
import { ErrorSummary, LocalAlert, VStack } from "@navikt/ds-react";
import { VilkårProvider, useVilkårContext } from "~/komponenter/behandling/vilkår/VilkårContext";
import { VilkårInnhold } from "~/komponenter/behandling/vilkår/VilkårInnhold";
import type { Route } from "./+types/vilkår";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import { vilkårNavn, vilkårNøkler } from "~/types/vilkår";

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
  const [harForsøktNeste, settHarForsøktNeste] = useState(false);
  const [fokusTeller, settFokusTeller] = useState(0);
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);

  const { status: vilkårStatus } = useVilkårContext();
  const manglendeVilkår = vilkårNøkler.filter(
    (nøkkel) => vilkårStatus.antallPerioder[nøkkel] === 0
  );

  // TODO: Øsker å nuke denne og flytte all steglogikk backend.
  const { navigerTilNeste } = useStegNavigering(STEG_PATH);

  useEffect(() => {
    if (fokusTeller > 0) {
      feiloppsummeringRef.current?.focus();
    }
  }, [fokusTeller]);

  const håndterNeste = () => {
    if (!vilkårStatus.alleVilkårErUtfylt) {
      settHarForsøktNeste(true);
      settFokusTeller((teller) => teller + 1);
      return;
    }
    navigerTilNeste();
  };

  return (
    <VStack gap={"space-24"}>
      {vilkårStatus.feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Content>{vilkårStatus.feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      <VilkårInnhold />

      {harForsøktNeste && manglendeVilkår.length > 0 && (
        <ErrorSummary
          ref={feiloppsummeringRef}
          heading="Du må vurdere disse vilkårene før du kan gå videre:"
        >
          {manglendeVilkår.map((nøkkel) => (
            <ErrorSummary.Item key={nøkkel} href={`#vilkar-${nøkkel}`}>
              {`${vilkårNavn[nøkkel]}: legg til minst én vurdert periode.`}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      <StegNavigering stegPath={STEG_PATH} onNeste={håndterNeste} />
    </VStack>
  );
}
