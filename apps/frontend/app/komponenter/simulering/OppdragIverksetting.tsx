import { BodyShort, LocalAlert, VStack } from "@navikt/ds-react";
import React, { useState } from "react";
import { hentOppdragStatus, iverksettMotOppdrag } from "~/api/generated/sdk.gen";
import type { OppdragIverksettingResponse, OppdragStatusSvar } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { formaterBelop, formaterIsoMånedÅr } from "~/utils/utils";

/**
 * Midlertidig: iverksetter behandlingen mot familie-oppdrag-backend for å teste integrasjonen i dev.
 * Endepunktet finnes ikke i prod.
 */
export const useOppdragIverksetting = (behandlingId: string) => {
  const [resultat, settResultat] = useState<OppdragIverksettingResponse | null>(null);
  const [status, settStatus] = useState<OppdragStatusSvar | null>(null);
  const [feilmelding, settFeilmelding] = useState<string | null>(null);
  const [iverksetter, settIverksetter] = useState(false);
  const [henterStatus, settHenterStatus] = useState(false);

  const iverksett = async () => {
    settIverksetter(true);
    settFeilmelding(null);
    settStatus(null);
    try {
      const { data } = await iverksettMotOppdrag({ path: { behandlingId }, throwOnError: true });
      settResultat(data);
    } catch (feil: unknown) {
      settResultat(null);
      settFeilmelding(feilmeldingFra(feil, "Iverksettingen mot Oppdrag feilet."));
    } finally {
      settIverksetter(false);
    }
  };

  const hentStatus = async () => {
    settHenterStatus(true);
    settFeilmelding(null);
    try {
      const { data } = await hentOppdragStatus({ path: { behandlingId }, throwOnError: true });
      settStatus(data);
    } catch (feil: unknown) {
      settStatus(null);
      settFeilmelding(feilmeldingFra(feil, "Vi kunne ikke hente status for oppdraget."));
    } finally {
      settHenterStatus(false);
    }
  };

  return { resultat, status, feilmelding, iverksetter, henterStatus, iverksett, hentStatus };
};

type OppdragIverksettingTilstand = ReturnType<typeof useOppdragIverksetting>;

const statusVisning = (
  status: string
): { alertStatus: "announcement" | "success" | "warning" | "error"; tittel: string } => {
  switch (status) {
    case "KVITTERT_OK":
      return { alertStatus: "success", tittel: "Oppdrag har kvittert OK" };
    case "LAGT_PÅ_KØ":
      return { alertStatus: "announcement", tittel: "Venter på kvittering fra Oppdrag" };
    case "KVITTERT_MED_MANGLER":
      return { alertStatus: "warning", tittel: "Oppdrag har kvittert med mangler" };
    default:
      return { alertStatus: "error", tittel: "Oppdrag har avvist oppdraget" };
  }
};

export const OppdragIverksettingResultat: React.FC<{ tilstand: OppdragIverksettingTilstand }> = ({
  tilstand,
}) => {
  const { resultat, status, feilmelding } = tilstand;

  if (!resultat && !status && !feilmelding) {
    return null;
  }

  return (
    <VStack gap="space-16">
      {feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Header>
            <LocalAlert.Title>Kallet mot Oppdrag feilet</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      {resultat && (
        <LocalAlert status="success">
          <LocalAlert.Header>
            <LocalAlert.Title>Oppdraget er sendt til Oppdrag</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            <VStack gap="space-8">
              <BodyShort>
                Saksnummer {resultat.saksnummer}, behandlings-ID {resultat.behandlingsId},{" "}
                {resultat.antallPerioder} {resultat.antallPerioder === 1 ? "periode" : "perioder"}.
              </BodyShort>
              {resultat.testandel && (
                <BodyShort>
                  Behandlingen har ingen tilkjent ytelse. Vi iverksatte med testandel på{" "}
                  {formaterBelop(resultat.testandel.beløp)} for{" "}
                  {formaterIsoMånedÅr(resultat.testandel.fom)}.
                </BodyShort>
              )}
              <BodyShort>Kvitteringen kommer etter en stund. Hent status for å se den.</BodyShort>
            </VStack>
          </LocalAlert.Content>
        </LocalAlert>
      )}

      {status && (
        <LocalAlert status={statusVisning(status.status).alertStatus}>
          <LocalAlert.Header>
            <LocalAlert.Title>{statusVisning(status.status).tittel}</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            Status: {status.status}
            {status.melding && `. ${status.melding}`}
          </LocalAlert.Content>
        </LocalAlert>
      )}
    </VStack>
  );
};
