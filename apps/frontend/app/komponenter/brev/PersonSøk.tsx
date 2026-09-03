import React, { useState } from "react";
import {
  BodyShort,
  Button,
  HStack,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { type Brevmottaker, BrevmottakerRolle, MottakerType } from "~/hooks/useBrevmottaker";

interface Props {
  leggTilMottaker: (mottaker: Brevmottaker) => void;
}

interface Feilmeldinger {
  personident?: string;
  mottakerRolle?: string;
}

export function PersonSøk({ leggTilMottaker }: Props) {
  const [personident, settPersonident] = useState("");
  const [mottakerRolle, settMottakerRolle] = useState<BrevmottakerRolle>();
  const [feilmeldinger, settFeilmeldinger] = useState<Feilmeldinger>({});

  const validerFelter = (): boolean => {
    const nyeFeilmeldinger: Feilmeldinger = {};

    if (!personident) {
      nyeFeilmeldinger.personident = "Personident er påkrevd";
    }
    if (!mottakerRolle) {
      nyeFeilmeldinger.mottakerRolle = "Mangler mottakerrolle";
    }

    settFeilmeldinger(nyeFeilmeldinger);
    return Object.keys(nyeFeilmeldinger).length === 0;
  };

  const håndterLeggTilPerson = () => {
    if (validerFelter()) {
      const nyPersonMottaker: Brevmottaker = {
        personident: personident,
        personRolle: mottakerRolle,
        mottakerType: MottakerType.PERSON,
      };
      leggTilMottaker(nyPersonMottaker);
      settPersonident("");
      settMottakerRolle(undefined);
      settFeilmeldinger({});
    }
  };

  return (
    <>
      <TextField
        label={"Personident"}
        htmlSize={26}
        placeholder={"Personen som skal ha brevet"}
        value={personident}
        onChange={(e) => settPersonident(e.target.value)}
        autoComplete="off"
        error={feilmeldinger.personident}
        style={{ maxWidth: "20rem" }}
      />
      {personident && (
        <VStack>
          <RadioGroup
            legend="Velg mottakerrolle"
            onChange={(rolle: BrevmottakerRolle) => settMottakerRolle(rolle)}
            value={mottakerRolle}
            error={feilmeldinger.mottakerRolle}
          >
            <Stack gap="space-0 space-24" direction={"row"} wrap={false}>
              <Radio value={BrevmottakerRolle.FULLMEKTIG}>Fullmektig</Radio>
              <Radio value={BrevmottakerRolle.VERGE}>Verge</Radio>
            </Stack>
          </RadioGroup>
          <HStack
            style={{ background: "var(--a-surface-subtle)" }}
            padding={"space-2"}
            gap={"space-24"}
            justify={"space-between"}
          >
            <VStack>
              <BodyShort>Hent navn her</BodyShort>
              <BodyShort>{personident}</BodyShort>
            </VStack>
            <Button
              style={{ width: "fit-content", alignSelf: "center", marginRight: "2rem" }}
              variant={"secondary"}
              size={"small"}
              onClick={() => {
                håndterLeggTilPerson();
              }}
            >
              Legg til
            </Button>
          </HStack>
        </VStack>
      )}
    </>
  );
}
