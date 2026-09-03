import type { FC } from "react";
import React from "react";
import { BodyShort, Box, Button, CopyButton, Heading, HStack, VStack } from "@navikt/ds-react";
import { type Brevmottaker, MottakerType } from "~/hooks/useBrevmottaker";
import { TrashIcon } from "@navikt/aksel-icons";

interface Props {
  mottakere: Brevmottaker[];
  fjernMottaker: (index: number) => void;
}

export const BrevmottakereListe: FC<Props> = ({ mottakere, fjernMottaker }) => {
  return (
    <VStack gap={"space-2"} width={"100%"}>
      <Heading level="2" size="xsmall">
        Brevmottakere
      </Heading>
      {mottakere.map((mottaker, index) => {
        const erOrganisasjon = mottaker.mottakerType === MottakerType.ORGANISASJON;
        const orgNrEllerPersonIdent = erOrganisasjon ? mottaker.orgnr : mottaker.personident;

        return (
          <Box key={index} background="neutral-soft" padding="space-4" borderRadius="2">
            <HStack justify="space-between" align="center">
              <VStack>
                <BodyShort>
                  {erOrganisasjon
                    ? `${mottaker.orgnr} v/ ${mottaker.navnHosOrganisasjon} (${mottaker.personRolle})`
                    : `${mottaker.personident} (${mottaker.personRolle})`}
                </BodyShort>
                <HStack gap="space-2" align="center">
                  <BodyShort size="small">{orgNrEllerPersonIdent}</BodyShort>
                  <CopyButton
                    size="xsmall"
                    copyText={orgNrEllerPersonIdent!}
                    variant="action"
                    activeText="kopiert"
                  />
                </HStack>
              </VStack>
              <Button
                variant="tertiary"
                onClick={() => fjernMottaker(index)}
                icon={<TrashIcon />}
              />
            </HStack>
          </Box>
        );
      })}
    </VStack>
  );
};
