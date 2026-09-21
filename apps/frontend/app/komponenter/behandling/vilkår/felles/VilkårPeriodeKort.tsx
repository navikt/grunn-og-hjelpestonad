import React from "react";
import { Button, FormSummary, HStack, Heading, Tag, VStack } from "@navikt/ds-react";
import { visPeriode } from "./useVilkårSkjema";

export interface Detalj {
  etikett: string;
  verdi: string;
}

export function VilkårPeriodeRedigering({
  fraOgMedDato,
  tilOgMedDato,
  children,
}: {
  fraOgMedDato?: string | null;
  tilOgMedDato?: string | null;
  children: React.ReactNode;
}) {
  return (
    <VStack gap="space-16">
      <Heading level="3" size="small">
        Endrer periode: {visPeriode(fraOgMedDato, tilOgMedDato)}
      </Heading>
      {children}
    </VStack>
  );
}

export function VilkårPeriodeKort({
  fraOgMedDato,
  tilOgMedDato,
  erVilkårOppfylt,
  detaljer,
  onEndre,
  onSlett,
  sletter = false,
}: {
  fraOgMedDato?: string | null;
  tilOgMedDato?: string | null;
  erVilkårOppfylt: boolean;
  detaljer: Detalj[];
  onEndre?: () => void;
  onSlett?: () => void;
  sletter?: boolean;
}) {
  const status = erVilkårOppfylt
    ? { tekst: "Oppfylt", farge: "success" as const }
    : { tekst: "Ikke oppfylt", farge: "danger" as const };

  return (
    <FormSummary>
      <FormSummary.Header>
        <HStack gap="space-12" align="center" wrap={false}>
          <FormSummary.Heading level="3">
            {visPeriode(fraOgMedDato, tilOgMedDato)}
          </FormSummary.Heading>
          <Tag variant="strong" size="small" data-color={status.farge}>
            {status.tekst}
          </Tag>
        </HStack>
      </FormSummary.Header>

      <FormSummary.Answers>
        {detaljer.map((detalj) => (
          <FormSummary.Answer key={detalj.etikett}>
            <FormSummary.Label>{detalj.etikett}</FormSummary.Label>
            <FormSummary.Value>{detalj.verdi}</FormSummary.Value>
          </FormSummary.Answer>
        ))}
      </FormSummary.Answers>

      {(onEndre || onSlett) && (
        <FormSummary.Footer>
          <HStack gap="space-8">
            {onEndre && (
              <Button type="button" variant="tertiary" size="small" onClick={onEndre}>
                Endre
              </Button>
            )}
            {onSlett && (
              <Button
                type="button"
                variant="tertiary"
                size="small"
                loading={sletter}
                onClick={onSlett}
              >
                Slett
              </Button>
            )}
          </HStack>
        </FormSummary.Footer>
      )}
    </FormSummary>
  );
}
