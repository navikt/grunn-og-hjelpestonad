import React from "react";
import { Heading, BodyShort, VStack, Alert } from "@navikt/ds-react";
import { useRouteLoaderData } from "react-router";
import type { Route } from "./+types/landingsside";
import type { Saksbehandler } from "~/server/types";
import { useToggles } from "~/hooks/useToggles";
import { ToggleNavn } from "~/types/toggles";
import { Side } from "~/komponenter/layout/Side";

export function meta(_: Route.MetaArgs) {
  return [
    { title: "Landingsside" },
    {
      name: "description",
      content: "Landingsside for saksbehandling av grunn- og hjelpestønad",
    },
  ];
}

export default function Landingsside() {
  const { saksbehandler } =
    useRouteLoaderData<{ saksbehandler: Saksbehandler | null }>("root") || {};

  const { toggles } = useToggles();

  return (
    <Side>
      <VStack gap="space-8">
        <Heading level="1" size="large" spacing>
          Grunn- og hjelpestønad
        </Heading>

        {toggles[ToggleNavn.TestToggle] && (
          <Alert variant="info">Hvis du ser denne er {ToggleNavn.TestToggle} togglet på.</Alert>
        )}

        {saksbehandler && (
          <VStack gap="space-4">
            <BodyShort spacing>Velkommen, {saksbehandler.navn}!</BodyShort>
          </VStack>
        )}
      </VStack>
    </Side>
  );
}
