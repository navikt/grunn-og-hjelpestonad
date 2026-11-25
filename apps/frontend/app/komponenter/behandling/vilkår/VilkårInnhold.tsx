import React from "react";
import { Accordion, LocalAlert, VStack } from "@navikt/ds-react";
import { MedlemskapVilkår } from "./medlemskap/MedlemskapVilkår";
import { DiagnoseVilkår } from "./diagnose/DiagnoseVilkår";
import { useVilkårContext } from "./VilkårContext";

export const VilkårInnhold: React.FC = () => {
  const { medlemskapFaktagrunnlagTilstand } = useVilkårContext();
  const harFalskIdentitet =
    medlemskapFaktagrunnlagTilstand.medlemskapFaktagrunnlag?.harFalskIdentitet;

  return (
    <VStack gap="space-24">
      {harFalskIdentitet === true && (
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Registrert med falsk identitet</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            Personen er registrert med falsk identitet i Folkeregisteret. Behandlingen kan ikke gå
            videre fra Vilkår så lenge registreringen gjelder.
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
