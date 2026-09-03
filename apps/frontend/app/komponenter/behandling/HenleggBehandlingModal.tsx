import React, { type RefObject } from "react";
import { Alert, BodyLong, Button, HStack, Modal } from "@navikt/ds-react";

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  laster: boolean;
  feilmelding: string | null;
  onHenlegg: () => Promise<void>;
}

export function HenleggBehandlingModal({ modalRef, laster, feilmelding, onHenlegg }: Props) {
  const lukkModal = () => modalRef.current?.close();

  return (
    <Modal ref={modalRef} header={{ heading: "Henlegg behandling" }}>
      <Modal.Body>
        <BodyLong>
          Dette vil henlegge behandlingen og sette den som ferdigstilt. Behandlingen kan ikke
          gjenåpnes etter henleggelse. Er du sikker på at du vil henlegge behandlingen?
        </BodyLong>
        {feilmelding && (
          <Alert variant="error" size="small" style={{ marginTop: "var(--ax-space-16)" }}>
            {feilmelding}
          </Alert>
        )}
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-4" justify="end">
          <Button variant="danger" onClick={onHenlegg} loading={laster}>
            Henlegg
          </Button>
          <Button variant="secondary" onClick={lukkModal} disabled={laster}>
            Avbryt
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
