import React from "react";
import { BodyShort, Box, Heading, Label, VStack } from "@navikt/ds-react";
import type { SimuleringResultat, SimuleringPeriode } from "~/routes/behandling/simulering";
import { formaterBelop, formaterIsoMånedÅr } from "~/utils/utils";

const Rad: React.FC<{ tittel: string; verdi: string; farge?: string }> = ({
  tittel,
  verdi,
  farge,
}) => (
  <div style={{ display: "flex", justifyContent: "space-between", gap: "2rem" }}>
    <BodyShort>{tittel}</BodyShort>
    <Label style={{ color: farge }}>{verdi}</Label>
  </div>
);

const NestePeriode: React.FC<{ nestePeriode: SimuleringPeriode }> = ({ nestePeriode }) => (
  <VStack gap="space-2">
    <Label>Neste utbetaling</Label>
    <Rad
      tittel={formaterIsoMånedÅr(nestePeriode.fom)}
      verdi={formaterBelop(nestePeriode.resultat)}
      farge={nestePeriode.resultat >= 0 ? "var(--a-green-600)" : "var(--a-red-600)"}
    />
  </VStack>
);

export const SimuleringOppsummering: React.FC<{ resultat: SimuleringResultat }> = ({
  resultat,
}) => {
  const { fom, tomSisteUtbetaling, fomDatoNestePeriode, etterbetaling, feilutbetaling, perioder } =
    resultat;

  const nestePeriode = fomDatoNestePeriode
    ? perioder.find((p) => p.fom === fomDatoNestePeriode)
    : undefined;

  const harHistoriskePerioder = fomDatoNestePeriode
    ? perioder.some((p) => p.fom < fomDatoNestePeriode)
    : perioder.length > 0;

  return (
    <VStack gap="space-4">
      <Heading size="medium" level="2">
        Simulering
      </Heading>
      <Box borderWidth="1" borderRadius="4" padding="space-8" style={{ display: "inline-block" }}>
        <VStack gap="space-4">
          {harHistoriskePerioder && (
            <VStack gap="space-2">
              <Label>
                Totalt for periode {formaterIsoMånedÅr(fom)} til og med{" "}
                {formaterIsoMånedÅr(tomSisteUtbetaling)}
              </Label>
              <Rad
                tittel="Feilutbetaling"
                verdi={feilutbetaling > 0 ? `-${formaterBelop(feilutbetaling)}` : formaterBelop(0)}
              />
              <Rad tittel="Etterbetaling" verdi={formaterBelop(etterbetaling)} />
            </VStack>
          )}
          {harHistoriskePerioder && nestePeriode && (
            <hr style={{ border: "none", borderTop: "1px solid var(--a-border-subtle)", margin: 0 }} />
          )}
          {nestePeriode && <NestePeriode nestePeriode={nestePeriode} />}
        </VStack>
      </Box>
    </VStack>
  );
};
