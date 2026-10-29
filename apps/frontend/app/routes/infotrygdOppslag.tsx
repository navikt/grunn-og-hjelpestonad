import React, { useState, type FormEvent } from "react";
import { Box, Button, HStack, TextField, VStack } from "@navikt/ds-react";
import type { Route } from "./+types/infotrygdOppslag";
import { InfotrygdOversikt } from "~/komponenter/infotrygd/InfotrygdOversikt";
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
    <Box flexGrow="1" minHeight="0" overflowY="auto">
      <Side>
        <VStack gap="space-8">
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

          {innsendtPersonident && (
            <InfotrygdOversikt key={innsendtPersonident} personident={innsendtPersonident} />
          )}
        </VStack>
      </Side>
    </Box>
  );
}
