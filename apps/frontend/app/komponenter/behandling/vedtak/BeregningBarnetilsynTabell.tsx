import React from "react";
import type {Beløpsperioder} from "~/komponenter/behandling/vedtak/vedtak";
import {Table} from "@navikt/ds-react";
import {format} from "date-fns";


export const BeregningBarnetilsynTabell: React.FC<{ beløpsperioder: Beløpsperioder }> = ({beløpsperioder}) => {
    const synligePerioder = beløpsperioder.filter(periode => periode.periodetype !== 'INGEN_STØNAD');
    
    return (
        <Table>
            <Table.Header>
                <Table.Row>
                    <Table.HeaderCell>Periode</Table.HeaderCell>
                    <Table.HeaderCell>Antall barn</Table.HeaderCell>
                    <Table.HeaderCell>Utgifter</Table.HeaderCell>
                    <Table.HeaderCell>Stønadsbeløp pr mnd</Table.HeaderCell>
                </Table.Row>
            </Table.Header>
            <Table.Body>
                {synligePerioder.map((periode, idx) => (
                    <Table.Row key={idx}>
                        <Table.DataCell>{format(periode.datoFra, 'MM-yyyy')} - {format(periode.datoTil, 'MM-yyyy')}</Table.DataCell>
                        <Table.DataCell>{periode.antallBarn}</Table.DataCell>
                        <Table.DataCell>{periode.utgifter}</Table.DataCell>
                        <Table.DataCell>{periode.beløp}</Table.DataCell>
                    </Table.Row>
                ))}
            </Table.Body>
        </Table>
    )
}