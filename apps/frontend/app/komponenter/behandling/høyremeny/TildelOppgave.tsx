import React, { useState } from "react";
import { Alert, BodyShort, Box, Button, VStack } from "@navikt/ds-react";
import { useRouteLoaderData } from "react-router";
import type { Saksbehandler } from "~/server/types";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import { useTildelOppgave } from "~/hooks/useTildelOppgave";

export const TildelOppgave: React.FC = () => {
  const { saksbehandler, env } =
    useRouteLoaderData<{
      saksbehandler: Saksbehandler | null;
      env: "lokalt" | "development" | "production";
    }>("root") || {};

  const {
    behandling,
    ansvarligSaksbehandler,
    lasterAnsvarligSaksbehandler,
    hentAnsvarligSaksbehandlerPåNytt,
  } = useBehandlingContext();
  const { tildelOppgave, laster, feilmelding } = useTildelOppgave();
  const [harTildelt, settHarTildelt] = useState(false);

  const erProduksjon = env === "production";
  const erInnloggetSaksbehandlerAnsvarlig =
    ansvarligSaksbehandler?.rolle === "INNLOGGET_SAKSBEHANDLER";

  const skalVises =
    behandling?.status !== "FERDIGSTILT" &&
    !erProduksjon &&
    !lasterAnsvarligSaksbehandler &&
    !erInnloggetSaksbehandlerAnsvarlig &&
    !harTildelt &&
    saksbehandler?.navIdent &&
    behandling?.id;

  if (!skalVises) {
    return null;
  }

  const handleTildelOppgave = async () => {
    if (!behandling?.id || !saksbehandler?.navIdent) return;

    const suksess = await tildelOppgave(behandling.id, saksbehandler.navIdent);
    if (suksess) {
      settHarTildelt(true);
      hentAnsvarligSaksbehandlerPåNytt();
    }
  };

  return (
    <Box borderColor="neutral" borderWidth="1" borderRadius="12" padding="space-16">
      <VStack gap="space-4">
        <BodyShort size="small" weight="semibold">
          KUN DEV - Overta oppgaven
        </BodyShort>
        <Button size="small" onClick={handleTildelOppgave} loading={laster}>
          Tildel oppgave
        </Button>
        {feilmelding && (
          <Alert size="small" variant="error">
            {feilmelding}
          </Alert>
        )}
      </VStack>
    </Box>
  );
};
