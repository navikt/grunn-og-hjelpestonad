import React, { useState, type FormEvent } from "react";
import { Alert, Button, Heading, HStack, Loader, TextField, VStack } from "@navikt/ds-react";
import type { Route } from "./+types/infotrygdOppslag";
import { useHentInfotrygdHistorikk } from "~/hooks/useHentInfotrygdHistorikk";
import { InfotrygdVedtak } from "~/komponenter/infotrygd/InfotrygdVedtak";
import { Side } from "~/komponenter/layout/Side";

export function meta(_: Route.MetaArgs) {
  return [
    { title: "Oppslag i Infotrygd" },
    {
      name: "description",
      content: "Oppslag av grunnstønad i Infotrygd på fødselsnummer",
    },
  ];
}

export default function InfotrygdOppslag() {
  const [inputverdi, settInputverdi] = useState("");
  const [innsendtPersonident, settInnsendtPersonident] = useState<string | undefined>(undefined);

  const håndterSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const renset = inputverdi.replace(/\s/g, "");
    if (renset === "") return;
    settInnsendtPersonident(renset);
  };

  return (
    <Side>
      <VStack gap="space-8">
        <Heading level="1" size="large" spacing>
          Oppslag i Infotrygd
        </Heading>

        <form onSubmit={håndterSubmit}>
          <HStack gap="space-8" align="end">
            <TextField
              label="Fødselsnummer"
              value={inputverdi}
              onChange={(e) => settInputverdi(e.target.value)}
              autoComplete="off"
              inputMode="numeric"
              style={{ maxWidth: "20rem" }}
            />
            <Button type="submit">Hent stønader</Button>
          </HStack>
        </form>

        {/* key nullstiller hooken per oppslag, så forrige persons data aldri vises. */}
        {innsendtPersonident && (
          <Oppslagsresultat key={innsendtPersonident} personident={innsendtPersonident} />
        )}
      </VStack>
    </Side>
  );
}

function Oppslagsresultat({ personident }: { personident: string }) {
  const historikkState = useHentInfotrygdHistorikk(personident);

  return historikkState.laster ? (
    <Loader title="Henter vedtak fra Infotrygd…" />
  ) : historikkState.melding ? (
    <Alert variant="warning">{historikkState.melding}</Alert>
  ) : historikkState.data != null ? (
    <InfotrygdVedtak data={historikkState.data} />
  ) : null;
}
