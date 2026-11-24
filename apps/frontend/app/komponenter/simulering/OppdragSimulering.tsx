import { BodyShort, Button, Heading, HStack, Loader, LocalAlert, VStack } from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import { hentSimulering, simulerMotOppdrag } from "~/api/generated/sdk.gen";
import type { SimuleringResponse } from "~/api/generated/types.gen";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { harManuellPosteringMedResultat } from "./formaterSimulering";
import { Simuleringsoppsummering } from "./Simuleringsoppsummering";
import { SimuleringTabell } from "./SimuleringTabell";

type SimuleringResultat =
  | { type: "mottatt"; simulering: SimuleringResponse }
  | { type: "hentingFeilet"; feilmelding: string };

type SimuleringForBehandling = {
  behandlingId: string;
  resultat: SimuleringResultat;
};

async function hentSimuleringResultat(behandlingId: string): Promise<SimuleringResultat> {
  try {
    const { data } = await hentSimulering({ path: { behandlingId }, throwOnError: true });
    return { type: "mottatt", simulering: data };
  } catch (feil: unknown) {
    return {
      type: "hentingFeilet",
      feilmelding: feilmeldingFra(
        feil,
        "Kunne ikke hente simuleringen mot Oppdrag. Last inn siden på nytt."
      ),
    };
  }
}

function SimuleringInnhold({
  simulering,
  erLesevisning,
}: {
  simulering: SimuleringResponse;
  erLesevisning: boolean;
}) {
  if (simulering.status === "IKKE_SIMULERT") {
    return (
      <LocalAlert status="announcement">
        <LocalAlert.Content>Behandlingen er ikke simulert ennå.</LocalAlert.Content>
      </LocalAlert>
    );
  }

  if (simulering.status === "INGEN_ENDRING") {
    return (
      <LocalAlert status="announcement">
        <LocalAlert.Content>
          Behandlingen endrer ikke utbetalingen, så den er ikke simulert mot Oppdrag.
        </LocalAlert.Content>
      </LocalAlert>
    );
  }

  if (simulering.status === "FEILET") {
    const feilmelding = erLesevisning
      ? "Simuleringen mot Oppdrag feilet."
      : "Simuleringen mot Oppdrag feilet. Prøv å simulere på nytt.";
    return (
      <LocalAlert status="error">
        <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
      </LocalAlert>
    );
  }

  const oppsummering = simulering.oppsummering;
  if (!oppsummering) {
    return <BodyShort>Simuleringen mangler oppsummering.</BodyShort>;
  }

  const ukjenteFagområdekoder = oppsummering.ukjenteFagområdekoder;
  const harManuellePosteringer = harManuellPosteringMedResultat(oppsummering.perioder);

  return (
    <VStack gap="space-16">
      {harManuellePosteringer && (
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Simuleringen har manuelle posteringer</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            Det er gjort manuelle posteringer tidligere. Du må mest sannsynlig sende en oppgave til
            NØS og be dem gjøre tilsvarende manuelle posteringer. Periodene det gjelder, er merket
            «Manuell postering».
          </LocalAlert.Content>
        </LocalAlert>
      )}
      {ukjenteFagområdekoder.length > 0 && (
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Simuleringen har posteringer vi ikke kjenner igjen</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            {`Det kan være manuelle posteringer. Sjekk posteringene før du går videre. Ukjente fagområdekoder: ${ukjenteFagområdekoder.join(", ")}.`}
          </LocalAlert.Content>
        </LocalAlert>
      )}
      <Simuleringsoppsummering
        oppsummering={oppsummering}
        simulertTidspunkt={simulering.simulertTidspunkt}
      />
      <SimuleringTabell perioder={oppsummering.perioder} />
    </VStack>
  );
}

export const OppdragSimulering: React.FC<{ behandlingId: string }> = ({ behandlingId }) => {
  const erLesevisning = useErLesevisning();
  const [simuleringForBehandling, settSimuleringForBehandling] =
    useState<SimuleringForBehandling | null>(null);
  const [simulerer, settSimulerer] = useState(false);
  const [simuleringsfeil, settSimuleringsfeil] = useState<string | null>(null);

  useEffect(() => {
    let erAvbrutt = false;
    void hentSimuleringResultat(behandlingId).then((resultat) => {
      if (!erAvbrutt) settSimuleringForBehandling({ behandlingId, resultat });
    });
    return () => {
      erAvbrutt = true;
    };
  }, [behandlingId]);

  const resultat =
    simuleringForBehandling?.behandlingId === behandlingId
      ? simuleringForBehandling.resultat
      : null;
  const laster = resultat === null;

  const simulerPåNytt = async () => {
    settSimulerer(true);
    settSimuleringsfeil(null);
    try {
      const { data } = await simulerMotOppdrag({ path: { behandlingId }, throwOnError: true });
      settSimuleringForBehandling({
        behandlingId,
        resultat: { type: "mottatt", simulering: data },
      });
    } catch (feil: unknown) {
      settSimuleringsfeil(
        feilmeldingFra(feil, "Simuleringen mot Oppdrag feilet. Prøv å simulere på nytt.")
      );
    } finally {
      settSimulerer(false);
    }
  };

  return (
    <VStack gap="space-16">
      <HStack gap="space-16" justify="space-between" align="center">
        <Heading size="small" level="2">
          Simulering mot Oppdrag
        </Heading>

        {!erLesevisning && (
          <Button
            variant="secondary"
            size="small"
            loading={simulerer}
            disabled={laster}
            onClick={() => void simulerPåNytt()}
          >
            Simuler på nytt
          </Button>
        )}
      </HStack>

      {simuleringsfeil && (
        <LocalAlert status="error">
          <LocalAlert.Header>
            <LocalAlert.Title>Simuleringen feilet</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>{simuleringsfeil}</LocalAlert.Content>
        </LocalAlert>
      )}

      {laster && <Loader title="Henter simulering mot Oppdrag" />}

      {resultat?.type === "hentingFeilet" && (
        <LocalAlert status="error">
          <LocalAlert.Content>{resultat.feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      {resultat?.type === "mottatt" && (
        <SimuleringInnhold simulering={resultat.simulering} erLesevisning={erLesevisning} />
      )}
    </VStack>
  );
};
