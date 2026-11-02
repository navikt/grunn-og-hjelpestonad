import { BodyShort, Button, Heading, LocalAlert, Table, VStack } from "@navikt/ds-react";
import React, { useState } from "react";
import { simulerMotOppdrag } from "~/api/generated/sdk.gen";
import type { OppdragSimuleringResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { formaterBelop, formaterIsoMånedÅr } from "~/utils/utils";

/**
 * Midlertidig visning for å teste integrasjonen mot familie-oppdrag-backend.
 * Viser posteringene fra Oppdrag slik de kommer, uten å tolke dem.
 */
export const OppdragSimulering: React.FC<{ behandlingId: string }> = ({ behandlingId }) => {
  const [resultat, settResultat] = useState<OppdragSimuleringResponse | null>(null);
  const [feilmelding, settFeilmelding] = useState<string | null>(null);
  const [simulerer, settSimulerer] = useState(false);

  const simuler = async () => {
    settSimulerer(true);
    settFeilmelding(null);
    try {
      const { data } = await simulerMotOppdrag({ path: { behandlingId }, throwOnError: true });
      settResultat(data);
    } catch (feil: unknown) {
      settResultat(null);
      settFeilmelding(feilmeldingFra(feil, "Simuleringen mot Oppdrag feilet."));
    } finally {
      settSimulerer(false);
    }
  };

  const posteringer =
    resultat?.resultat.simuleringMottaker.flatMap((mottaker) =>
      mottaker.simulertPostering.map((postering) => ({ ...postering, mottaker }))
    ) ?? [];

  return (
    <VStack gap="space-16">
      <Heading size="small" level="2">
        Simulering mot Oppdrag (test)
      </Heading>
      <div>
        <Button variant="secondary" loading={simulerer} onClick={() => void simuler()}>
          Simuler mot Oppdrag
        </Button>
      </div>

      {feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Header>
            <LocalAlert.Title>Simuleringen feilet</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>{feilmelding}</LocalAlert.Content>
        </LocalAlert>
      )}

      {resultat?.testandel && (
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Simulert med testandel</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            Behandlingen har ingen tilkjent ytelse. Vi simulerte med{" "}
            {formaterBelop(resultat.testandel.beløp)} for{" "}
            {formaterIsoMånedÅr(resultat.testandel.fom)}.
          </LocalAlert.Content>
        </LocalAlert>
      )}

      {resultat && posteringer.length === 0 && (
        <BodyShort>Oppdrag svarte uten posteringer.</BodyShort>
      )}

      {posteringer.length > 0 && (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
              <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
              <Table.HeaderCell scope="col">Mottakertype</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fagområde</Table.HeaderCell>
              <Table.HeaderCell scope="col">Klassekode</Table.HeaderCell>
              <Table.HeaderCell scope="col">Posteringstype</Table.HeaderCell>
              <Table.HeaderCell scope="col">Betalingstype</Table.HeaderCell>
              <Table.HeaderCell scope="col">Forfallsdato</Table.HeaderCell>
              <Table.HeaderCell scope="col" align="right">
                Beløp
              </Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {posteringer.map((postering, indeks) => (
              <Table.Row key={indeks}>
                <Table.DataCell>{postering.fom ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.tom ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.mottaker.mottakerType ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.fagOmrådeKode ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.klassekode ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.posteringType ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.betalingType ?? "–"}</Table.DataCell>
                <Table.DataCell>{postering.forfallsdato ?? "–"}</Table.DataCell>
                <Table.DataCell align="right">
                  {postering.beløp != null ? formaterBelop(postering.beløp) : "–"}
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </VStack>
  );
};
