import React from "react";
import { Alert, Heading, Loader, VStack } from "@navikt/ds-react";
import type { Route } from "./+types/infotrygdHistorikk";
import { useHentInfotrygdHistorikk } from "~/hooks/useHentInfotrygdHistorikk";
import { usePersonContext } from "~/contexts/PersonContext";

export function meta(_: Route.MetaArgs) {
  return [
    { title: "Historikk i Infotrygd" },
    {
      name: "description",
      content: "Oversikt over historikk fra Infotrygd",
    },
  ];
}

export default function InfotrygdHistorikk(_: Route.ComponentProps) {
  const { personident } = usePersonContext();
  const state = useHentInfotrygdHistorikk(personident);
  const { data: historikk, melding, laster } = state;

  if (laster) {
    return (
      <div>
        Laster historikk fra Infotrygd...
        <Loader title="Laster..." />
      </div>
    );
  }

  return (
    <VStack gap="space-4">
      <Heading level="1" size="medium" spacing>
        Historikk i infotrygd for personident: {personident}
      </Heading>

      {melding && <Alert variant="warning">{melding}</Alert>}

      {historikk != null && (
        <pre
          style={{
            backgroundColor: "#f5f5f5",
            padding: "1rem",
            borderRadius: "4px",
            overflow: "auto",
          }}
        >
          {JSON.stringify(historikk, null, 2)}
        </pre>
      )}
    </VStack>
  );
}
