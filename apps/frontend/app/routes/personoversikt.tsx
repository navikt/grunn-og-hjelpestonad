import React from "react";
import { VStack, BodyShort } from "@navikt/ds-react";
import type { Route } from "./+types/personoversikt";
import { usePersonContext } from "~/fellesContext/PersonContext";
import { formaterNavn } from "~/utils/utils";

export function meta(_: Route.MetaArgs) {
  return [{ title: `Personoversikt` }];
}

export default function PersonOversikt() {
  const { personident, person } = usePersonContext();
  const visningsNavn = person?.navn ? formaterNavn(person.navn) : "Navn ikke tilgjengelig";

  return (
    <VStack gap="space-4">
      <BodyShort size="large" weight="semibold">
        {visningsNavn}
      </BodyShort>
      <BodyShort>Personident: {personident || "Ikke tilgjengelig"}</BodyShort>
    </VStack>
  );
}
