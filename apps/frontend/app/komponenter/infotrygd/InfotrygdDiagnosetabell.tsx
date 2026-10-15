import React from "react";
import { BodyShort, Table } from "@navikt/ds-react";
import type { InfotrygdDiagnoseResponse as InfotrygdDiagnose } from "~/api/generated/types.gen";

interface InfotrygdDiagnosetabellProps {
  diagnoser: InfotrygdDiagnose[];
}

const maanedsformat = new Intl.DateTimeFormat("nb-NO", {
  month: "long",
  year: "numeric",
  timeZone: "UTC",
});

const formaterMaaned = (maanedNotert: string): string => {
  const treff = /^(\d{4})-(\d{2})$/.exec(maanedNotert);
  if (!treff) return maanedNotert;

  const aar = Number(treff[1]);
  const maaned = Number(treff[2]);
  if (maaned < 1 || maaned > 12) return maanedNotert;

  try {
    return maanedsformat.format(new Date(Date.UTC(aar, maaned - 1, 1)));
  } catch {
    return maanedNotert;
  }
};

const visStonadstype = (stonadstype: InfotrygdDiagnose["stønadstype"]): string => {
  return stonadstype === "GRUNNSTØNAD" ? "Grunnstønad" : "Hjelpestønad";
};

export function InfotrygdDiagnosetabell({ diagnoser }: InfotrygdDiagnosetabellProps) {
  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Stønadstype</Table.HeaderCell>
          <Table.HeaderCell scope="col">Registrert måned</Table.HeaderCell>
          <Table.HeaderCell scope="col">Kode</Table.HeaderCell>
          <Table.HeaderCell scope="col">Diagnose (ICD-10)</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {diagnoser.length === 0 ? (
          <Table.Row>
            <Table.DataCell colSpan={4}>
              <BodyShort size="small" textColor="subtle">
                Ingen diagnoser registrert i Infotrygd
              </BodyShort>
            </Table.DataCell>
          </Table.Row>
        ) : (
          diagnoser.map((diagnose) => (
            <Table.Row
              key={`${diagnose.stønadstype}-${diagnose.månedNotert}-${diagnose.diagnosekode}`}
            >
              <Table.DataCell>{visStonadstype(diagnose.stønadstype)}</Table.DataCell>
              <Table.DataCell>{formaterMaaned(diagnose.månedNotert)}</Table.DataCell>
              <Table.DataCell>{diagnose.diagnosekode}</Table.DataCell>
              <Table.DataCell>
                {diagnose.icd10Tekst ?? (
                  <BodyShort size="small" textColor="subtle">
                    Ukjent ICD-10-kode
                  </BodyShort>
                )}
              </Table.DataCell>
            </Table.Row>
          ))
        )}
      </Table.Body>
    </Table>
  );
}
