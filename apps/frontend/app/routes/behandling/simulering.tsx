import { Box, Button, Loader, VStack } from "@navikt/ds-react";
import React, { useCallback, useEffect, useState } from "react";
import type { Route } from "./+types/simulering";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import { apiCall, type ApiResponse } from "~/api/backend";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
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

export interface SimuleringResultat {
  perioder: SimuleringPeriode[];
  fom: string;
  tomSisteUtbetaling: string;
  fomDatoNestePeriode: string | null;
  etterbetaling: number;
  feilutbetaling: number;
}

export interface SimuleringPeriode {
  fom: string;
  tom: string;
  nyttBeløp: number;
  tidligereUtbetalt: number;
  resultat: number;
  feilutbetaling: number;
}

const hentSimulertResultat = async (
  behandlingId: string
): Promise<ApiResponse<SimuleringResultat>> => {
  return await apiCall(`/simulering/${behandlingId}/resultat`, {
    method: "GET",
  });
};

export default function Simulering() {
  const { behandlingId } = useBehandlingContext();
  const { harForrigeSteg, navigerTilNeste } = useStegNavigering(STEG_PATH);
  const [simuleringResultat, settSimuleringResultat] = useState<SimuleringResultat | null>(null);
  const [statusMelding, settStatusMelding] = useState<string>("");
  const [laster, settLaster] = useState(true);

  const handleHentResultat = useCallback(async () => {
    const resultat = await hentSimulertResultat(behandlingId);
    if (resultat.data) {
      settSimuleringResultat(resultat.data);
      settStatusMelding("Simulering fullført");
      settLaster(false);
      return true;
    }
    return false;
  }, [behandlingId]);

  useEffect(() => {
    let avbrutt = false;

    const hentVedInnlasting = async () => {
      const fantSimuleringResultat = await handleHentResultat();
      if (fantSimuleringResultat || avbrutt) return;

      await new Promise((resolve) => setTimeout(resolve, 3000));
      if (!avbrutt) {
        settLaster(false);
        settStatusMelding("Simulering er ikke ferdig. Prøv igjen om litt");
      }
    };

    hentVedInnlasting();
    return () => {
      avbrutt = true;
    };
  }, [behandlingId, handleHentResultat]);

  if (laster) {
    return <Loader size="medium" title="Henter simuleringsresultat..." />;
  }

  return (
    <VStack gap={"space-24"}>
      <Box>
        {!simuleringResultat && (
          <Button variant="secondary" onClick={handleHentResultat}>
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
