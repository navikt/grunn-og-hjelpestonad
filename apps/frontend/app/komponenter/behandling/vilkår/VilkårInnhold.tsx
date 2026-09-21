import React from "react";
import { Accordion, VStack } from "@navikt/ds-react";
import { MedlemskapVilkår } from "./medlemskap/MedlemskapVilkår";
import { DiagnoseVilkår } from "./diagnose/DiagnoseVilkår";
import { InstitusjonVilkår } from "./institusjon/InstitusjonVilkår";

export const VilkårInnhold: React.FC = () => {
  return (
    <VStack gap="space-24">
      <Accordion>
        <MedlemskapVilkår />
        <DiagnoseVilkår />
        <InstitusjonVilkår />
      </Accordion>
    </VStack>
  );
};
