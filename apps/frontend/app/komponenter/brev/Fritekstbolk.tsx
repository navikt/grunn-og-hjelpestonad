import { Button, HStack, Textarea, TextField, VStack } from "@navikt/ds-react";
import React from "react";
import { ArrowDownIcon, ArrowUpIcon, TrashIcon } from "@navikt/aksel-icons";
import type { Tekstbolk } from "~/komponenter/brev/typer";
import { useErLesevisning } from "~/hooks/useErLesevisning";

interface Props {
  underoverskrift?: string;
  innhold: string;
  handleOppdaterFelt: (value: Partial<{ underoverskrift: string; innhold: string }>) => void;
  handleFlyttOpp: () => void;
  handleFlyttNed: () => void;
  handleSlett: () => void;
  fritekstfeltListe: Tekstbolk[];
}

export const Fritekstbolk = ({
  underoverskrift,
  innhold,
  handleOppdaterFelt,
  handleFlyttOpp,
  handleFlyttNed,
  handleSlett,
  fritekstfeltListe,
}: Props) => {
  const erLesevisning = useErLesevisning();

  return (
    <VStack gap="space-16">
      <TextField
        label="Deloverskrift"
        value={underoverskrift}
        onChange={(e) => handleOppdaterFelt({ underoverskrift: e.target.value })}
        size="small"
        readOnly={erLesevisning}
      />
      <Textarea
        label="Innhold"
        value={innhold}
        onChange={(e) => handleOppdaterFelt({ innhold: e.target.value })}
        size="small"
        minRows={3}
        readOnly={erLesevisning}
      />
      {fritekstfeltListe.length > 1 && (
        <HStack justify="space-between">
          <HStack>
            <Button
              variant="tertiary"
              icon={<ArrowUpIcon />}
              onClick={handleFlyttOpp}
              size="small"
              disabled={erLesevisning}
            />
            <Button
              variant="tertiary"
              icon={<ArrowDownIcon />}
              onClick={handleFlyttNed}
              size="small"
              disabled={erLesevisning}
            />
          </HStack>
          <Button
            variant="tertiary"
            icon={<TrashIcon />}
            onClick={handleSlett}
            size="small"
            disabled={erLesevisning}
          />
        </HStack>
      )}
    </VStack>
  );
};
