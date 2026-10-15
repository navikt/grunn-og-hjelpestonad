import React from "react";
import { Alert, Heading, Loader, VStack } from "@navikt/ds-react";
import type { Route } from "./+types/infotrygdHistorikk";
import { useHentInfotrygdHistorikk } from "~/hooks/useHentInfotrygdHistorikk";
import { useHentInfotrygdDiagnoser } from "~/hooks/useHentInfotrygdDiagnoser";
import { usePersonContext } from "~/fellesContext/PersonContext";
import { InfotrygdDiagnosetabell } from "~/komponenter/infotrygd/InfotrygdDiagnosetabell";

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
  const diagnoserState = useHentInfotrygdDiagnoser(personident);
  const historikkState = useHentInfotrygdHistorikk(personident);

  return (
    <VStack gap="space-4">
      <Heading level="1" size="medium" spacing>
        Historikk i infotrygd for personident: {personident}
      </Heading>

      <Heading level="2" size="small">
        Diagnoser
      </Heading>
      {diagnoserState.laster ? (
        <Loader title="Henter diagnoser fra Infotrygd…" />
      ) : diagnoserState.melding ? (
        <Alert variant="warning">{diagnoserState.melding}</Alert>
      ) : (
        <InfotrygdDiagnosetabell diagnoser={diagnoserState.data?.diagnoser ?? []} />
      )}

      <Heading level="2" size="small">
        Perioder
      </Heading>
      {historikkState.laster ? (
        <Loader title="Henter perioder fra Infotrygd…" />
      ) : historikkState.melding ? (
        <Alert variant="warning">{historikkState.melding}</Alert>
      ) : historikkState.data != null ? (
        <pre
          style={{
            backgroundColor: "#f5f5f5",
            padding: "1rem",
            borderRadius: "4px",
            overflow: "auto",
          }}
        >
          {JSON.stringify(historikkState.data, null, 2)}
        </pre>
      ) : null}
    </VStack>
  );
}
