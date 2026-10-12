import React from "react";
import { Alert, BodyLong, Button, HStack, Modal } from "@navikt/ds-react";
import type { BeslutterRespons } from "~/hooks/useBeslutter";

interface Props {
  modalRef: React.RefObject<HTMLDialogElement | null>;
  laster: boolean;
  feilmelding: string | null;
  onSendTilBeslutter: () => Promise<BeslutterRespons>;
}

export function SendTilBeslutterModal({ modalRef, laster, feilmelding, onSendTilBeslutter }: Props) {
  const lukkModal = () => modalRef.current?.close();

  return (
    <Modal ref={modalRef} header={{ heading: "Totrinnskontroll" }}>
      <Modal.Body>
        <BodyLong>
          Du er i ferd med å sende behandlingen til totrinnskontroll. Vil du fortsette?
        </BodyLong>
      </Modal.Body>
      <Modal.Footer>
        <HStack width={"100%"} gap="space-16" justify={"space-between"}>
          <Button variant="secondary" onClick={lukkModal} disabled={laster}>
            Avbryt
          </Button>
          <Button
            onClick={async () => {
              const respons = await onSendTilBeslutter();
              if (respons.data) lukkModal();
            }}
            disabled={laster}
          >
            Send til beslutter
          </Button>
        </HStack>
        {feilmelding && (
          <Alert variant="error" size="small">
            {feilmelding}
          </Alert>
        )}
      </Modal.Footer>
    </Modal>
  );
}
