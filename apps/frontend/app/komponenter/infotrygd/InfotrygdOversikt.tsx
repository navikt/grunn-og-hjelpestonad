import React from "react";
import { Heading, Loader, LocalAlert, VStack } from "@navikt/ds-react";
import { useHentGjeldendeInfotrygdPerioder } from "~/hooks/useHentGjeldendeInfotrygdPerioder";
import { useHentInfotrygdHistorikk } from "~/hooks/useHentInfotrygdHistorikk";
import { GjeldendeInfotrygdPerioder } from "./GjeldendeInfotrygdPerioder";
import { InfotrygdVedtak } from "./InfotrygdVedtak";

export function InfotrygdOversikt({ personident }: { personident: string | undefined }) {
  const gjeldendeState = useHentGjeldendeInfotrygdPerioder(personident);
  const historikkState = useHentInfotrygdHistorikk(personident);

  return (
    <VStack gap="space-4">
      <Heading level="1" size="medium" spacing>
        Grunnstønad i Infotrygd for personident: {personident}
      </Heading>

      {gjeldendeState.laster ? (
        <Loader title="Henter gjeldende perioder fra Infotrygd…" />
      ) : gjeldendeState.melding ? (
        <LocalAlert status="warning">
          <LocalAlert.Content>{gjeldendeState.melding}</LocalAlert.Content>
        </LocalAlert>
      ) : gjeldendeState.data != null ? (
        <GjeldendeInfotrygdPerioder data={gjeldendeState.data} />
      ) : null}

      {historikkState.laster ? (
        <Loader title="Henter vedtak fra Infotrygd…" />
      ) : historikkState.melding ? (
        <LocalAlert status="warning">
          <LocalAlert.Content>{historikkState.melding}</LocalAlert.Content>
        </LocalAlert>
      ) : historikkState.data != null ? (
        <InfotrygdVedtak
          data={historikkState.data}
          gjeldendePerioder={gjeldendeState.data?.perioder ?? []}
        />
      ) : null}
    </VStack>
  );
}
