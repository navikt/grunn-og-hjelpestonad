import { Button, HGrid, HStack, Modal, VStack } from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import { BrevmottakereListe } from "~/komponenter/brev/BrevmottakereListe";
import { type Brevmottaker } from "~/hooks/useBrevmottaker";
import { ManueltSøk } from "~/komponenter/brev/ManueltSøk";
import { SkalBrukerMottaBrev } from "~/komponenter/brev/SkalBrukerMottaBrev";
import { Skillelinje } from "~/komponenter/layout/Skillelinje";
import { useBehandlingContext } from "~/contexts/BehandlingContext";

interface Props {
  mottakere: Brevmottaker[];
  settMottakere: (mottakere: Brevmottaker[]) => void;
  lukkModal: () => void;
  sendMottakereTilSak: (behandlingId: string, mottakere: Brevmottaker[]) => void;
}

export default function BrevmottakerModalInnhold({
  mottakere,
  settMottakere,
  lukkModal,
  sendMottakereTilSak,
}: Props) {
  const [midlertidigMottakerliste, settMidlertidigMottakerliste] = useState<Brevmottaker[]>([]);
  const { behandlingId } = useBehandlingContext();

  useEffect(() => {
    settMidlertidigMottakerliste([...mottakere]);
  }, [mottakere]);

  const leggTilMottaker = (mottaker: Brevmottaker) => {
    settMidlertidigMottakerliste((prev) => [...prev, mottaker]);
  };

  const fjernMottaker = (index: number) => {
    settMidlertidigMottakerliste((prev) => prev.filter((_, i) => i !== index));
  };

  const håndterSettMottakere = () => {
    settMottakere(midlertidigMottakerliste);
    sendMottakereTilSak(behandlingId, midlertidigMottakerliste);

    lukkModal();
  };

  const håndterAvbryt = () => {
    settMidlertidigMottakerliste([...mottakere]);
    lukkModal();
  };

  const harEndringer = JSON.stringify(mottakere) !== JSON.stringify(midlertidigMottakerliste);

  return (
    <>
      <Modal.Body>
        <HGrid columns={2} gap="space-24">
          <VStack gap="space-6">
            <ManueltSøk leggTilMottaker={leggTilMottaker} />
            <Skillelinje />
            <SkalBrukerMottaBrev
              mottakere={midlertidigMottakerliste}
              leggTilMottaker={leggTilMottaker}
              fjernMottaker={fjernMottaker}
            />
          </VStack>
          <VStack
            gap="space-4"
            style={{
              borderLeft: "1px solid var(--ax-border-neutral-subtle)",
              paddingLeft: "var(--ax-space-8)",
            }}
          >
            <BrevmottakereListe
              mottakere={midlertidigMottakerliste}
              fjernMottaker={fjernMottaker}
            />
          </VStack>
        </HGrid>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-4" justify="end">
          <Button variant="secondary" onClick={håndterAvbryt}>
            Avbryt
          </Button>
          <Button
            onClick={håndterSettMottakere}
            disabled={!harEndringer || midlertidigMottakerliste.length === 0}
          >
            Sett mottakere
          </Button>
        </HStack>
      </Modal.Footer>
    </>
  );
}
