import React from "react";
import { BodyShort, Box, Heading, HStack, Table, Tag, VStack } from "@navikt/ds-react";
import type { InfotrygdGjeldendePerioderResponse } from "~/api/generated/types.gen";
import { erMotstridende, ingenBryting, TomtFelt, UlikeVerdier, VerdiMedKilde } from "./celler";
import { formaterDato, formaterPeriode } from "./formatering";
import { nyesteGhFørst } from "./vedtaksrader";

type GjeldendePeriode = InfotrygdGjeldendePerioderResponse["perioder"][number];

const KOLONNER = ["Periode", "Sats", "Trygdetid", "Diagnose", "Bokstav i § 6-3", "Vedtatt"];

const nyesteFomFørst = (a: GjeldendePeriode, b: GjeldendePeriode): number =>
  b.fom.localeCompare(a.fom);

function PeriodeRad({ periode }: { periode: GjeldendePeriode }) {
  const { vedtak } = periode;
  const nyesteGh = nyesteGhFørst(vedtak.gh?.verdi ?? [])[0];
  const ghKilde = vedtak.gh?.arvetFraVedtak ?? null;
  const evaluering = vedtak.evaluering?.verdi ?? null;
  const evalueringKilde = vedtak.evaluering?.arvetFraVedtak ?? null;
  const sats = nyesteGh?.satsGs;
  const hjemmelbokstav = evaluering?.hjemmelbokstav?.trim();
  const diagnosekoder = [evaluering?.diagnose1, evaluering?.diagnose2]
    .map((kode) => kode?.trim())
    .filter(Boolean)
    .join(", ");
  const ulikeDiagnoser =
    erMotstridende(evaluering, "DIAGNOSE_1") || erMotstridende(evaluering, "DIAGNOSE_2");

  return (
    <Table.Row>
      <Table.DataCell>
        <HStack gap="space-4" align="center" wrap={false}>
          <span style={ingenBryting}>{formaterPeriode(periode.fom, periode.tom)}</span>
          {!periode.tom && (
            <Tag variant="moderate" size="xsmall" data-color="info">
              Løpende
            </Tag>
          )}
        </HStack>
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
          <UlikeVerdier kilde={evalueringKilde} />
        ) : diagnosekoder ? (
          <VerdiMedKilde kilde={evalueringKilde}>{diagnosekoder}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>
        {erMotstridende(evaluering, "HJEMMELBOKSTAV") ? (
          <UlikeVerdier kilde={evalueringKilde} />
        ) : hjemmelbokstav ? (
          <VerdiMedKilde kilde={evalueringKilde}>{hjemmelbokstav}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>{formaterDato(vedtak.tidspunktReg)}</Table.DataCell>
    </Table.Row>
  );
}

export function GjeldendeInfotrygdPerioder({ data }: { data: InfotrygdGjeldendePerioderResponse }) {
  const perioder = data.perioder.toSorted(nyesteFomFørst);
  const overskriftId = "infotrygd-gjeldende-perioder";

  return (
    <VStack gap="space-8">
      <Heading level="2" size="small" id={overskriftId}>
        Gjeldende perioder
      </Heading>
      {perioder.length === 0 ? (
        <BodyShort>Ingen gjeldende perioder på grunnstønad i Infotrygd</BodyShort>
      ) : (
        <Box overflowX="auto" tabIndex={0}>
          <Table size="small" aria-labelledby={overskriftId}>
            <Table.Header>
              <Table.Row>
                {KOLONNER.map((tittel) => (
                  <Table.HeaderCell key={tittel} scope="col" style={ingenBryting}>
                    {tittel}
                  </Table.HeaderCell>
                ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {perioder.map((periode) => (
                <PeriodeRad
                  key={`${String(periode.stonadId)}-${periode.vedtak.vedtakId}-${periode.fom}`}
                  periode={periode}
                />
              ))}
            </Table.Body>
          </Table>
        </Box>
      )}
    </VStack>
  );
}
