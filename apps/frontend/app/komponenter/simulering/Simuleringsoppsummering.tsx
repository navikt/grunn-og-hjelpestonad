import { BodyShort, Box, Detail, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import React from "react";
import type { Simuleringsoppsummering as SimuleringsoppsummeringDto } from "~/api/generated/types.gen";
import { formaterBelop } from "~/utils/utils";
import {
  formaterBeløpEllerStrek,
  formaterMånedLang,
  formaterOppsummeringTittel,
  formaterSimulertTidspunkt,
  medStorForbokstav,
} from "./formaterSimulering";

type Props = {
  oppsummering: SimuleringsoppsummeringDto;
  simulertTidspunkt?: string | null;
};

export const Simuleringsoppsummering: React.FC<Props> = ({ oppsummering, simulertTidspunkt }) => {
  const harFeilutbetaling = oppsummering.feilutbetaling > 0;
  const nesteUtbetaling = oppsummering.nesteUtbetaling;

  return (
    <VStack gap="space-8">
      <Heading level="3" size="small">
        Oppsummering
      </Heading>
      <Box maxWidth="26rem" borderColor="neutral-strong" borderWidth="1" padding="space-40">
        <VStack aria-label="Simuleringsoversikt" gap="space-12">
          <BodyShort weight="semibold">
            {formaterOppsummeringTittel(oppsummering.fom, oppsummering.tomSisteUtbetaling)}
          </BodyShort>
          <HStack>
            <BodyShort>Feilutbetaling</BodyShort>
            <Spacer />
            <BodyShort
              weight="semibold"
              textColor={harFeilutbetaling ? "subtle" : "default"}
              data-color={harFeilutbetaling ? "danger" : "neutral"}
            >
              {formaterBeløpEllerStrek(oppsummering.feilutbetaling)}
            </BodyShort>
          </HStack>
          <HStack>
            <BodyShort>Etterbetaling</BodyShort>
            <Spacer />
            <BodyShort weight="semibold">
              {formaterBeløpEllerStrek(oppsummering.etterbetaling)}
            </BodyShort>
          </HStack>
          <Box borderColor="neutral" borderWidth="1 0 0 0">
            <HStack paddingBlock="space-16 space-0">
              <BodyShort weight="semibold">Neste utbetaling</BodyShort>
              <Spacer />
              {!nesteUtbetaling && <BodyShort weight="semibold">–</BodyShort>}
            </HStack>
          </Box>
          {nesteUtbetaling && (
            <HStack>
              <BodyShort>{medStorForbokstav(formaterMånedLang(nesteUtbetaling.fom))}</BodyShort>
              <Spacer />
              <BodyShort
                weight="semibold"
                textColor={nesteUtbetaling.beløp > 0 ? "subtle" : "default"}
                data-color={nesteUtbetaling.beløp > 0 ? "success" : "neutral"}
              >
                {formaterBelop(nesteUtbetaling.beløp)}
              </BodyShort>
            </HStack>
          )}
          <Detail textColor="subtle">
            Simulert {formaterSimulertTidspunkt(simulertTidspunkt)}
          </Detail>
        </VStack>
      </Box>
    </VStack>
  );
};
