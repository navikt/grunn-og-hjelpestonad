import React from "react";
import { Button, HStack } from "@navikt/ds-react";
import type { Brevmal, Tekstbolk } from "~/komponenter/brev/typer";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { useBehandlingContext } from "~/contexts/BehandlingContext";

interface Props {
  brevMal: Brevmal | null;
  fritekstbolker: Tekstbolk[];
  behandlingId: string;
  sender: boolean;
  sendPdfTilSak: (behandlingId: string, brevmal: Brevmal, fritekstbolker: Tekstbolk[]) => void;
  senderTilBeslutter: boolean;
  handleSendTilBeslutter: () => void;
  erSendtTilBeslutter: boolean;
  harForrigeSteg: boolean;
  navigerTilForrige: () => void;
}

export function BrevHandlinger({
  brevMal,
  fritekstbolker,
  behandlingId,
  sender,
  sendPdfTilSak,
  senderTilBeslutter,
  handleSendTilBeslutter,
  erSendtTilBeslutter,
  harForrigeSteg,
  navigerTilForrige,
}: Props) {
  const erLesevisning = useErLesevisning();

  const { ansvarligSaksbehandler } = useBehandlingContext();

  const erAnsvarligSaksbehandler = ansvarligSaksbehandler?.rolle === "INNLOGGET_SAKSBEHANDLER";

  return (
    <HStack justify="space-between" flexShrink="0">
      {harForrigeSteg && (
        <Button variant="secondary" onClick={navigerTilForrige}>
          Tilbake
        </Button>
      )}
      {brevMal && (
        <HStack gap="space-24">
          <Button
            variant="secondary"
            onClick={() => sendPdfTilSak(behandlingId, brevMal, fritekstbolker)}
            disabled={sender || erLesevisning}
          >
            Send PDF til sak
          </Button>
          <Button
            onClick={handleSendTilBeslutter}
            disabled={
              senderTilBeslutter ||
              erLesevisning ||
              erSendtTilBeslutter ||
              !erAnsvarligSaksbehandler
            }
          >
            Send til beslutter
          </Button>
        </HStack>
      )}
    </HStack>
  );
}
