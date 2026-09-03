import React from "react";
import { Table } from "@navikt/ds-react";
import type { SimuleringResultat } from "~/routes/behandling/simulering";
import { formaterBelop, formaterIsoMånedÅr } from "~/utils/utils";

export const SimuleringTabell: React.FC<{ resultat: SimuleringResultat }> = ({ resultat }) => {
  const periodeData = resultat.perioder.map((periode) => ({
    fom: periode.fom,
    måned: formaterIsoMånedÅr(periode.fom),
    nyttBeløp: periode.nyttBeløp,
    tidligereUtbetalt: periode.tidligereUtbetalt,
    resultat: periode.resultat,
    gjelderNestePeriode: periode.fom === resultat.fomDatoNestePeriode,
  }));

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          {periodeData.map((p) => (
            <Table.HeaderCell key={p.fom}>{p.måned}</Table.HeaderCell>
          ))}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.HeaderCell>Nytt beløp</Table.HeaderCell>
          {periodeData.map((p) => (
            <Table.DataCell key={p.fom}>{formaterBelop(p.nyttBeløp)}</Table.DataCell>
          ))}
        </Table.Row>
        <Table.Row>
          <Table.HeaderCell>Tidligere utbetalt</Table.HeaderCell>
          {periodeData.map((p) => (
            <Table.DataCell key={p.fom}>{formaterBelop(p.tidligereUtbetalt)}</Table.DataCell>
          ))}
        </Table.Row>
        <Table.Row>
          <Table.HeaderCell>Resultat</Table.HeaderCell>
          {periodeData.map((p) => (
            <Table.DataCell key={p.fom}>
              {p.resultat !== 0 && (
                <span
                  style={{ color: p.resultat > 0 ? "var(--a-green-600)" : "var(--a-red-600)" }}
                >
                  {formaterBelop(p.resultat)}
                </span>
              )}
            </Table.DataCell>
          ))}
        </Table.Row>
      </Table.Body>
    </Table>
  );
};
