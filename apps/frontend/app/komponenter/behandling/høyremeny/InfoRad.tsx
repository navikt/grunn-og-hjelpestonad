import { HStack, BodyShort } from "@navikt/ds-react";
import React from "react";

export const InfoRad = ({ label, verdi }: { label: string; verdi: string }) => (
  <HStack gap="space-6" align="center" justify="space-between">
    <BodyShort size={"small"} weight={"semibold"}>
      {label}
    </BodyShort>
    <BodyShort size={"small"}>{verdi}</BodyShort>
  </HStack>
);
