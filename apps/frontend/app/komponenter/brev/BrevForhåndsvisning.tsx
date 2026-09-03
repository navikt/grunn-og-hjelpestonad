import React from "react";
import { Box, VStack } from "@navikt/ds-react";
import type { Brevmal, Tekstbolk } from "~/komponenter/brev/typer";
import { PdfForhåndsvisning } from "~/komponenter/brev/PdfForhåndsvisning";

interface Props {
  brevMal: Brevmal | null;
  fritekstbolker: Tekstbolk[];
}

export function BrevForhåndsvisning({ brevMal, fritekstbolker }: Props) {
  return (
    <Box overflow="hidden" borderRadius="2" background="neutral-soft" minHeight="0">
      {brevMal ? (
        <PdfForhåndsvisning brevmal={brevMal} fritekstbolker={fritekstbolker} />
      ) : (
        <VStack align="center" justify="center" height="100%" />
      )}
    </Box>
  );
}
