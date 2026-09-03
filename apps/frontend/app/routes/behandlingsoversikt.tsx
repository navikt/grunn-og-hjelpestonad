import React, { forwardRef } from "react";
import {
  Button,
  Heading,
  Loader,
  VStack,
  Table,
  Tag,
  type DataCellProps,
  Alert,
} from "@navikt/ds-react";
import type { Route } from "./+types/behandlingsoversikt";
import { useHentBehandlinger } from "~/hooks/useHentBehandlinger";
import { useParams } from "react-router";
import { usePersonContext } from "~/contexts/PersonContext";
import { useOpprettBehandling } from "~/hooks/useOpprettBehandling";
import { useNavigate } from "react-router";
import { formaterIsoDatoTid, formatterEnumVerdi } from "~/utils/utils";
import type { Behandling } from "~/types/behandling";
import type { TagFarge } from "~/types/farge";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Behandlingsoversikt" }];
}

export const TableDataCellSmall = forwardRef<HTMLTableCellElement, DataCellProps>((props, ref) => (
  <Table.DataCell textSize={"small"} {...props} ref={ref} />
));
TableDataCellSmall.displayName = "TableDataCellSmall";

const behandlingStatusTagFarge: Record<Behandling["status"], TagFarge> = {
  OPPRETTET: "info",
  UTREDES: "info",
  FATTER_VEDTAK: "warning",
  IVERKSETTER_VEDTAK: "meta-lime",
  FERDIGSTILT: "success",
};

export default function Behandlingsoversikt() {
  const { fagsakPersonId } = useParams<{ fagsakPersonId: string }>();
  const navigate = useNavigate();
  const { opprettBehandling, opprettFeilmelding, oppretter } = useOpprettBehandling();
  const { fagsak, fagsakId } = usePersonContext();
  const { behandlinger, laster } = useHentBehandlinger(fagsakId);

  const behandlingerSortert = behandlinger
    ? [...behandlinger].sort(
        (a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime()
      )
    : [];

  if (laster || !behandlinger || !fagsak) {
    return (
      <div>
        Henter data...
        <Loader title="Laster..." />
      </div>
    );
  }

  const startOpprettBehandling = async () => {
    if (fagsak) {
      const behandlingId = await opprettBehandling(fagsak.id);
      if (behandlingId) {
        gåTilBehandling(behandlingId);
      }
    }
  };

  const gåTilBehandling = (behandlingId: string) => {
    navigate(`/person/${fagsakPersonId}/behandling/${behandlingId}/arsak-behandling`);
  };

  return (
    <VStack gap="space-24">
      <Heading level="1" size="large">
        Behandlingsoversikt
      </Heading>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Behandling opprettetdato</Table.HeaderCell>
            <Table.HeaderCell>Opprettet av</Table.HeaderCell>
            <Table.HeaderCell>Status</Table.HeaderCell>
            <Table.HeaderCell>Resultat</Table.HeaderCell>
            <Table.HeaderCell></Table.HeaderCell>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {behandlingerSortert.map((behandling) => (
            <Table.Row key={behandling.id}>
              <TableDataCellSmall>{formaterIsoDatoTid(behandling.opprettet)}</TableDataCellSmall>
              <TableDataCellSmall>{behandling.opprettetAv}</TableDataCellSmall>
              <TableDataCellSmall>
                <Tag
                  variant="moderate"
                  size="xsmall"
                  data-color={behandlingStatusTagFarge[behandling.status]}
                >
                  {formatterEnumVerdi(behandling.status)}
                </Tag>
              </TableDataCellSmall>
              <TableDataCellSmall>{formatterEnumVerdi(behandling.resultat)}</TableDataCellSmall>
              <TableDataCellSmall>
                <Button
                  size={"small"}
                  variant={"secondary"}
                  onClick={() => gåTilBehandling(behandling.id)}
                >
                  Åpne behandling
                </Button>
              </TableDataCellSmall>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>

      <VStack gap={"space-8"}>
        <div>
          <Button onClick={startOpprettBehandling} disabled={oppretter}>
            Lag behandling
          </Button>
        </div>

        {opprettFeilmelding && (
          <Alert variant="warning" size="small">
            {opprettFeilmelding}
          </Alert>
        )}
      </VStack>
    </VStack>
  );
}
