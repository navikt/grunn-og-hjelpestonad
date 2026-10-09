import React from "react";
import { BodyShort, Detail, HStack, Table } from "@navikt/ds-react";
import type {
  MedlemskapFaktagrunnlagResponse,
  OpplysningResponse,
} from "~/api/generated/types.gen";

interface Opplysningsgruppe {
  tittel: string;
  kilde: string;
  opplysninger: OpplysningResponse[];
}

interface MedlemskapOpplysningstabellProps {
  medlemskapFaktagrunnlag: MedlemskapFaktagrunnlagResponse;
  visOpplysningsperiode: (opplysning: OpplysningResponse) => string;
}

export function MedlemskapOpplysningstabell({
  medlemskapFaktagrunnlag,
  visOpplysningsperiode,
}: MedlemskapOpplysningstabellProps) {
  const grupper: Opplysningsgruppe[] = [
    {
      tittel: "Bosted",
      kilde: "Folkeregisteret",
      opplysninger: medlemskapFaktagrunnlag.bosted,
    },
    {
      tittel: "Statsborgerskap",
      kilde: "Folkeregisteret",
      opplysninger: medlemskapFaktagrunnlag.statsborgerskap,
    },
    {
      tittel: "Oppholdstillatelse",
      kilde: "UDI via Folkeregisteret",
      opplysninger: medlemskapFaktagrunnlag.oppholdstillatelse,
    },
  ];

  return (
    <Table size="small">
      {grupper.map((gruppe) => (
        <Table.Body key={gruppe.tittel}>
          <Table.Row>
            <Table.HeaderCell scope="rowgroup" colSpan={2}>
              <HStack gap="space-8" align="baseline">
                {gruppe.tittel}
                <Detail as="span" textColor="subtle">
                  {gruppe.kilde}
                </Detail>
              </HStack>
            </Table.HeaderCell>
          </Table.Row>
          {gruppe.opplysninger.length === 0 ? (
            <Table.Row>
              <Table.DataCell colSpan={2}>
                <BodyShort size="small" textColor="subtle">
                  Ingen opplysninger
                </BodyShort>
              </Table.DataCell>
            </Table.Row>
          ) : (
            gruppe.opplysninger.map((opplysning, indeks) => (
              <Table.Row key={`${opplysning.fraOgMedDato}-${opplysning.beskrivelse}-${indeks}`}>
                <Table.DataCell>{visOpplysningsperiode(opplysning)}</Table.DataCell>
                <Table.DataCell>{opplysning.beskrivelse}</Table.DataCell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      ))}
    </Table>
  );
}
