import { Heading, Radio, RadioGroup, VStack } from "@navikt/ds-react";
import React from "react";
import { usePersonContext } from "~/contexts/PersonContext";
import { type Brevmottaker, BrevmottakerRolle, MottakerType } from "~/hooks/useBrevmottaker";

interface Props {
  mottakere: Brevmottaker[];
  leggTilMottaker: (mottaker: Brevmottaker) => void;
  fjernMottaker: (index: number) => void;
}

export function SkalBrukerMottaBrev({ mottakere, leggTilMottaker, fjernMottaker }: Props) {
  const { personident } = usePersonContext();
  const brukerSkalHaBrev = mottakere.some(
    (mottaker) => mottaker.personRolle === BrevmottakerRolle.BRUKER
  );

  const håndterLeggTilBruker = () => {
    if (!brukerSkalHaBrev) {
      leggTilMottaker({
        mottakerType: MottakerType.PERSON,
        personRolle: BrevmottakerRolle.BRUKER,
        personident: personident,
      });
    }
  };

  const håndterFjernBruker = () => {
    const brukerIndex = mottakere.findIndex(
      (mottaker) => mottaker.personRolle === BrevmottakerRolle.BRUKER
    );
    if (brukerIndex !== -1) {
      fjernMottaker(brukerIndex);
    }
  };

  return (
    <VStack gap={"space-6"}>
      <Heading level="2" size="xsmall">
        Skal bruker motta brevet?
      </Heading>
      <RadioGroup
        legend={"Skal bruker motta brevet?"}
        hideLegend
        value={brukerSkalHaBrev ? "Ja" : "Nei"}
      >
        <Radio value={"Ja"} name={"brukerHaBrevRadio"} onChange={håndterLeggTilBruker}>
          Ja
        </Radio>
        <Radio value={"Nei"} name={"brukerHaBrevRadio"} onChange={håndterFjernBruker}>
          Nei
        </Radio>
      </RadioGroup>
    </VStack>
  );
}
