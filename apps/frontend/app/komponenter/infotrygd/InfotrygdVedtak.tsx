import React from "react";
import { BodyShort, Box, Heading, Table, VStack } from "@navikt/ds-react";
import type { InfotrygdStonaderResponse } from "~/api/generated/types.gen";
import { formaterDato, formaterMaaned } from "./formatering";
import {
  Beløp,
  erMotstridende,
  ingenBryting,
  Periode,
  TomtFelt,
  UlikeVerdier,
  VerdiMedKilde,
} from "./celler";
import { Detaljer } from "./Detaljer";
import { lagVedtaksrader, type Vedtaksrad } from "./vedtaksrader";

function Radvisning({
  rad,
  diagnosetekster,
}: {
  rad: Vedtaksrad;
  diagnosetekster: Record<string, string>;
}) {
  const { vedtak, evaluering } = rad;
  const nyesteGh = rad.gh[0];
  const sats = nyesteGh?.satsGs;
  const ghKilde = rad.ghFraVedtak;
  const evalueringskilde = rad.evalueringFraVedtak;
  const virkningstidspunkt = evaluering?.virkningstidspunkt;
  const hjemmelbokstav = evaluering?.hjemmelbokstav?.trim();
  const diagnosekoder = [evaluering?.diagnose1, evaluering?.diagnose2]
    .map((kode) => kode?.trim())
    .filter(Boolean)
    .join(", ");
  const ulikeDiagnoser =
    erMotstridende(evaluering, "DIAGNOSE_1") || erMotstridende(evaluering, "DIAGNOSE_2");

  return (
    <Table.ExpandableRow
      content={<Detaljer rad={rad} diagnosetekster={diagnosetekster} />}
      togglePlacement="left"
    >
      <Table.DataCell>
        <Periode rad={rad} />
      </Table.DataCell>
      <Table.DataCell>
        <Beløp rad={rad} />
      </Table.DataCell>
      <Table.DataCell>
        {sats != null ? <VerdiMedKilde kilde={ghKilde}>{sats}</VerdiMedKilde> : <TomtFelt />}
      </Table.DataCell>
      <Table.DataCell>
        {nyesteGh ? (
          <VerdiMedKilde kilde={ghKilde}>{`${nyesteGh.trygdetid} år`}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>
        {ulikeDiagnoser ? (
          <UlikeVerdier kilde={evalueringskilde} />
        ) : diagnosekoder ? (
          <VerdiMedKilde kilde={evalueringskilde}>{diagnosekoder}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>
        {erMotstridende(evaluering, "HJEMMELBOKSTAV") ? (
          <UlikeVerdier kilde={evalueringskilde} />
        ) : hjemmelbokstav ? (
          <VerdiMedKilde kilde={evalueringskilde}>{hjemmelbokstav}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>
        {erMotstridende(evaluering, "VIRKNINGSTIDSPUNKT") ? (
          <UlikeVerdier kilde={evalueringskilde} />
        ) : virkningstidspunkt ? (
          <VerdiMedKilde kilde={evalueringskilde}>
            {formaterMaaned(virkningstidspunkt)}
          </VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>{formaterDato(vedtak.datoMottattSak)}</Table.DataCell>
      <Table.DataCell>{formaterDato(vedtak.tidspunktReg)}</Table.DataCell>
      <Table.DataCell>{vedtak.brukerid}</Table.DataCell>
    </Table.ExpandableRow>
  );
}

const KOLONNER = [
  "Periode",
  "Beløp",
  "Sats",
  "Trygdetid",
  "Diagnose",
  "Bokstav i § 6-3",
  "Virkningstidspunkt",
  "Søknad mottatt",
  "Vedtatt",
  "Saksbehandler",
];

export function InfotrygdVedtak({ data }: { data: InfotrygdStonaderResponse }) {
  const rader = lagVedtaksrader(data.stonader);
  const overskriftId = "infotrygd-grunnstonad";

  return (
    <VStack gap="space-8">
      <Heading level="2" size="small" id={overskriftId}>
        Grunnstønad
      </Heading>
      {rader.length === 0 ? (
        <BodyShort>Ingen vedtak på grunnstønad i Infotrygd</BodyShort>
      ) : (
        <Box overflowX="auto" tabIndex={0}>
          <Table size="small" aria-labelledby={overskriftId}>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                {KOLONNER.map((tittel) => (
                  <Table.HeaderCell key={tittel} scope="col" style={ingenBryting}>
                    {tittel}
                  </Table.HeaderCell>
                ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {rader.map((rad) => (
                <Radvisning
                  key={rad.vedtak.vedtakId}
                  rad={rad}
                  diagnosetekster={data.diagnosetekster}
                />
              ))}
            </Table.Body>
          </Table>
        </Box>
      )}
    </VStack>
  );
}
