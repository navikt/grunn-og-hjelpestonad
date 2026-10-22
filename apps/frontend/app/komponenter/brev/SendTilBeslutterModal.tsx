import React from "react";
import { BodyLong, Button, HStack, LocalAlert, Modal } from "@navikt/ds-react";

interface Props {
  modalRef: React.RefObject<HTMLDialogElement | null>;
  laster: boolean;
  feilmelding: string | null;
  onSendTilBeslutter: () => Promise<void>;
}

export function SendTilBeslutterModal({
  modalRef,
  laster,
  feilmelding,
  onSendTilBeslutter,
}: Props) {
  const lukkModal = () => modalRef.current?.close();

  return (
    <Modal ref={modalRef} header={{ heading: "Totrinnskontroll" }}>
      <Modal.Body>
        <BodyLong>
          Du er i ferd med å sende behandlingen til totrinnskontroll. Vil du fortsette?
        </BodyLong>
        {feilmelding && (
          <LocalAlert status="error" size="small">
            <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
          </LocalAlert>
        )}
      </Modal.Body>
      <Modal.Footer>
        <HStack width={"100%"} gap="space-16" justify={"space-between"}>
          <Button variant="secondary" onClick={lukkModal} disabled={laster}>
            Avbryt
          </Button>
          <Button onClick={onSendTilBeslutter} loading={laster}>
            Send til beslutter
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
