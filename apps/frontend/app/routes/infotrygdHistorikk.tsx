import React from "react";
import { Alert, Heading, Loader, VStack } from "@navikt/ds-react";
import type { Route } from "./+types/infotrygdHistorikk";
import { useHentInfotrygdHistorikk } from "~/hooks/useHentInfotrygdHistorikk";
import { usePersonContext } from "~/fellesContext/PersonContext";
import { InfotrygdVedtak } from "~/komponenter/infotrygd/InfotrygdVedtak";

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
  const historikkState = useHentInfotrygdHistorikk(personident);

  return (
    <VStack gap="space-4">
      <Heading level="1" size="medium" spacing>
        Historikk i Infotrygd for personident: {personident}
      </Heading>

      {historikkState.laster ? (
        <Loader title="Henter vedtak fra Infotrygd…" />
      ) : historikkState.melding ? (
        <Alert variant="warning">{historikkState.melding}</Alert>
      ) : historikkState.data != null ? (
        <InfotrygdVedtak data={historikkState.data} />
      ) : null}
    </VStack>
  );
}
