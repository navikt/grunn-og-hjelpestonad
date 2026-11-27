import React from "react";
import { Accordion, LocalAlert, VStack } from "@navikt/ds-react";
import { MedlemskapVilkår } from "./medlemskap/MedlemskapVilkår";
import { DiagnoseVilkår } from "./diagnose/DiagnoseVilkår";
import { useVilkårContext } from "./VilkårContext";

export const VilkårInnhold: React.FC = () => {
  const { falskIdentitetTilstand } = useVilkårContext();
  const { harFalskIdentitet, error: falskIdentitetFeil } = falskIdentitetTilstand;

  return (
    <VStack gap="space-24">
      {falskIdentitetFeil !== undefined && (
        <LocalAlert status="error">
          <LocalAlert.Content>Kunne ikke hente status for falsk identitet.</LocalAlert.Content>
        </LocalAlert>
      )}
      {harFalskIdentitet === true && (
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Registrert med falsk identitet</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            Personen er registrert med falsk identitet i Folkeregisteret. Behandlingen kan ikke gå
            videre fra fanen Inngangsvilkår så lenge registreringen gjelder.
          </LocalAlert.Content>
        </LocalAlert>
      )}
      <Accordion>
        <MedlemskapVilkår />
        <DiagnoseVilkår />
      </Accordion>
    </VStack>
  );
};
