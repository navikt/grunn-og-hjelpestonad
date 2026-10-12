import React from "react";
import { BodyLong, Button, HStack, LocalAlert, Modal } from "@navikt/ds-react";
import type { BeslutterRespons } from "~/hooks/useBeslutter";

interface Props {
  modalRef: React.RefObject<HTMLDialogElement | null>;
  laster: boolean;
  feilmelding: string | null;
  onSendTilBeslutter: () => Promise<BeslutterRespons>;
  onAvbryt: () => void;
}

export function SendTilBeslutterModal({ modalRef, laster, feilmelding, onSendTilBeslutter, onAvbryt }: Props) {
  const lukkModal = () => modalRef.current?.close();

  return (
    <Modal ref={modalRef} header={{ heading: "Totrinnskontroll" }} onClose={onAvbryt}>
      <Modal.Body>
        <BodyLong>
          Du er i ferd med å sende behandlingen til totrinnskontroll. Vil du fortsette?
        </BodyLong>
      </Modal.Body>
      <Modal.Footer>
        <HStack width={"100%"} gap="space-16" justify={"space-between"}>
          <Button variant="secondary" onClick={() => { onAvbryt(); lukkModal(); }} disabled={laster}>
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
          <LocalAlert status="error" size="small">
            <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
          </LocalAlert>
        )}
      </Modal.Footer>
    </Modal>
  );
}
