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
import { PlusCircleIcon } from "@navikt/aksel-icons";

interface Props {
  leggTilMottaker: (mottaker: Brevmottaker) => void;
}

interface Feilmeldinger {
  organisasjonsnummer?: string;
  kontaktpersonHosOrganisasjon?: string;
  mottakerRolle?: string;
}

export const OrganisasjonsSøk: React.FC<Props> = ({ leggTilMottaker }) => {
  const [organisasjonsnummer, settOrganisasjonsnummer] = useState("");
  const [kontaktpersonHosOrganisasjon, settKontaktpersonHosOrganisasjon] = useState("");
  const [mottakerRolle, settMottakerRolle] = useState<BrevmottakerRolle>();
  const [feilmeldinger, settFeilmeldinger] = useState<Feilmeldinger>({});

  const validerFelter = (): boolean => {
    const nyeFeilmeldinger: Feilmeldinger = {};

    if (organisasjonsnummer.length !== 9) {
      nyeFeilmeldinger.organisasjonsnummer = "Organisasjonsnummer må være 9 siffer";
    }
    if (!kontaktpersonHosOrganisasjon) {
      nyeFeilmeldinger.kontaktpersonHosOrganisasjon = "Mangler kontaktperson hos organisasjon";
    }
    if (!mottakerRolle) {
      nyeFeilmeldinger.mottakerRolle = "Mangler mottakerrolle";
    }

    settFeilmeldinger(nyeFeilmeldinger);
    return Object.keys(nyeFeilmeldinger).length === 0;
  };

  const håndterLeggTilOrganisasjon = () => {
    if (validerFelter()) {
      const nyOrganisasjonMottaker: Brevmottaker = {
        personRolle: mottakerRolle,
        mottakerType: MottakerType.ORGANISASJON,
        orgnr: organisasjonsnummer,
        navnHosOrganisasjon: kontaktpersonHosOrganisasjon,
      };
      leggTilMottaker(nyOrganisasjonMottaker);
      settOrganisasjonsnummer("");
      settKontaktpersonHosOrganisasjon("");
      settMottakerRolle(undefined);
      settFeilmeldinger({});
    }
  };

  return (
    <>
      <TextField
        label={"Organisasjonsnummer"}
        htmlSize={26}
        placeholder={"Søk"}
        value={organisasjonsnummer}
        onChange={(e) => settOrganisasjonsnummer(e.target.value)}
        autoComplete="off"
        error={feilmeldinger.organisasjonsnummer}
        style={{ maxWidth: "20rem" }}
      />
      {organisasjonsnummer && (
        <VStack>
          <RadioGroup
            legend="Velg mottakerrolle"
            onChange={(rolle: BrevmottakerRolle) => settMottakerRolle(rolle)}
            value={mottakerRolle}
            error={feilmeldinger.mottakerRolle}
          >
            <Stack gap="space-0 space-24" direction={"row"} wrap={false}>
              <Radio value={BrevmottakerRolle.FULLMEKTIG}>Fullmektig</Radio>
              <Radio value={BrevmottakerRolle.ANNEN}>Annen mottaker</Radio>
            </Stack>
          </RadioGroup>
          <HStack
            style={{ background: "var(--a-surface-subtle)" }}
            padding={"space-2"}
            justify={"space-between"}
          >
            <VStack gap={"space-1"}>
              <BodyShort>Orgnavn</BodyShort>
              <BodyShort>orgnr</BodyShort>
              <TextField
                htmlSize={25}
                label={"Ved"}
                placeholder={"Personen brevet skal til"}
                value={kontaktpersonHosOrganisasjon}
                onChange={(e) => settKontaktpersonHosOrganisasjon(e.target.value)}
                autoComplete="off"
                error={feilmeldinger.kontaktpersonHosOrganisasjon}
              />
            </VStack>
            <Button
              icon={<PlusCircleIcon />}
              style={{ width: "fit-content", alignSelf: "flex-start" }}
              variant={"secondary"}
              size={"medium"}
              onClick={() => {
                håndterLeggTilOrganisasjon();
              }}
            >
              Legg til
            </Button>
          </HStack>
        </VStack>
      )}
    </>
  );
};
