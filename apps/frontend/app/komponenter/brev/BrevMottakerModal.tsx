import { Button, HGrid, HStack, LocalAlert, Modal, VStack } from "@navikt/ds-react";
import React, { useState } from "react";
import { BrevmottakereListe } from "~/komponenter/brev/BrevmottakereListe";
import { type Brevmottaker } from "~/hooks/useBrevmottaker";
import { ManueltSøk } from "~/komponenter/brev/ManueltSøk";
import { SkalBrukerMottaBrev } from "~/komponenter/brev/SkalBrukerMottaBrev";
import { Skillelinje } from "~/komponenter/layout/Skillelinje";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import type { ApiResponse } from "~/api/backend";

interface Props {
  mottakere: Brevmottaker[];
  settMottakere: (mottakere: Brevmottaker[]) => void;
  lukkModal: () => void;
  sendMottakereTilSak: (
    behandlingId: string,
    mottakere: Brevmottaker[]
  ) => Promise<ApiResponse>;
}

export default function BrevmottakerModalInnhold({
  mottakere,
  settMottakere,
  lukkModal,
  sendMottakereTilSak,
}: Props) {
  const [midlertidigMottakerliste, settMidlertidigMottakerliste] =
    useState<Brevmottaker[]>(mottakere);
  const [feilmelding, settFeilmelding] = useState<string>();
  const [lagrer, settLagrer] = useState(false);
  const { behandlingId } = useBehandlingContext();

  const leggTilMottaker = (mottaker: Brevmottaker) => {
    settMidlertidigMottakerliste((prev) => [...prev, mottaker]);
  };

  const fjernMottaker = (index: number) => {
    settMidlertidigMottakerliste((prev) => prev.filter((_, i) => i !== index));
  };

  const håndterSettMottakere = async () => {
    settLagrer(true);
    settFeilmelding(undefined);
    try {
      const response = await sendMottakereTilSak(behandlingId, midlertidigMottakerliste);
      if (response.melding || response.httpStatus) {
        settFeilmelding(response.melding || "Kunne ikke lagre brevmottakerne.");
        return;
      }

      settMottakere(midlertidigMottakerliste);
      lukkModal();
    } catch {
      settFeilmelding("Kunne ikke lagre brevmottakerne.");
    } finally {
      settLagrer(false);
    }
  };

  const håndterAvbryt = () => {
    settMidlertidigMottakerliste([...mottakere]);
    settFeilmelding(undefined);
    lukkModal();
  };

  const harEndringer = JSON.stringify(mottakere) !== JSON.stringify(midlertidigMottakerliste);

  return (
    <>
      <Modal.Body>
        {feilmelding && (
          <LocalAlert status="error" size="small">
            <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
          </LocalAlert>
        )}
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
            disabled={!harEndringer || midlertidigMottakerliste.length === 0 || lagrer}
          >
            {lagrer ? "Lagrer …" : "Sett mottakere"}
          </Button>
        </HStack>
      </Modal.Footer>
    </>
  );
}
