import { Box, Button, Loader, VStack } from "@navikt/ds-react";
import React, { useCallback, useEffect, useState } from "react";
import type { Route } from "./+types/simulering";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import { hentSimuleringsresultat } from "~/api/generated/sdk.gen";
import type { SimuleringResultatResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { SimuleringTabell } from "~/komponenter/simulering/SimuleringTabell";
import { SimuleringOppsummering } from "~/komponenter/simulering/SimuleringOppsummering";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";

export function meta(_args: Route.MetaArgs) {
  return [
    { title: "Simulering" },
    {
      name: "Simulering",
      content: "Simuleringsside",
    },
  ];
}

const STEG_PATH: StegPath = "simulering";

export type SimuleringResultat = SimuleringResultatResponse;
export type SimuleringPeriode = SimuleringResultatResponse["perioder"][number];

export default function Simulering() {
  const { behandlingId } = useBehandlingContext();
  const { harForrigeSteg, navigerTilNeste } = useStegNavigering(STEG_PATH);
  const [simuleringResultat, settSimuleringResultat] = useState<SimuleringResultatResponse | null>(
    null
  );
  const [statusMelding, settStatusMelding] = useState<string>("");
  const [laster, settLaster] = useState(true);

  const handleHentResultat = useCallback(
    async (signal?: AbortSignal): Promise<"funnet" | "feil" | "mangler" | "avbrutt"> => {
      try {
        const { data, response } = await hentSimuleringsresultat({
          path: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal?.aborted) return "avbrutt";
        if (response.status === 204) return "mangler";
        settSimuleringResultat(data);
        settStatusMelding("Simulering fullført");
        settLaster(false);
        return "funnet";
      } catch (feil: unknown) {
        if (signal?.aborted) return "avbrutt";
        settStatusMelding(
          feilmeldingFra(feil, "Vi kunne ikke hente simuleringsresultatet. Prøv igjen senere.")
        );
        settLaster(false);
        return "feil";
      }
    },
    [behandlingId]
  );

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentVedInnlasting = async () => {
      const resultat = await handleHentResultat(signal);
      if (resultat !== "mangler" || signal.aborted) return;

      await new Promise((resolve) => setTimeout(resolve, 3000));
      if (!signal.aborted) {
        settLaster(false);
        settStatusMelding("Simulering er ikke ferdig. Prøv igjen om litt");
      }
    };

    void hentVedInnlasting();
    return () => controller.abort();
  }, [handleHentResultat]);

  if (laster) {
    return <Loader size="medium" title="Henter simuleringsresultat..." />;
  }

  return (
    <VStack gap={"space-24"}>
      <Box>
        {!simuleringResultat && (
          <Button variant="secondary" onClick={() => void handleHentResultat()}>
            Hent simuleringsresultat
          </Button>
        )}
        {simuleringResultat ? (
          <VStack gap="space-8">
            <SimuleringOppsummering resultat={simuleringResultat} />
            <SimuleringTabell resultat={simuleringResultat} />
          </VStack>
        ) : (
          <p>{statusMelding}</p>
        )}
      </Box>
      {harForrigeSteg && (
        <Box>
          <StegNavigering stegPath={STEG_PATH} onNeste={navigerTilNeste} />
        </Box>
      )}
    </VStack>
  );
}
