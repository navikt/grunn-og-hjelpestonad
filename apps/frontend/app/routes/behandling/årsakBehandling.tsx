import React, { useEffect } from "react";
import type { Route } from "./+types/årsakBehandling";
import {
  Alert,
  Box,
  Button,
  DatePicker,
  Loader,
  Select,
  Textarea,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMarkerStegFerdige } from "~/hooks/useMarkerStegFerdige";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import { useArsakBehandling } from "~/hooks/useÅrsakBehandling";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import type { ÅrsakType } from "~/types/årsak";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { RedigerOgSlettKnapper } from "~/komponenter/behandling/RedigerOgSlettKnapper";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Årsak behandling" }];
}

const STEG_NAVN = "Årsak behandling";
const STEG_PATH: StegPath = "arsak-behandling";

const ÅRSAK_ALTERNATIVER = [
  { verdi: "SØKNAD", label: "Søknad" },
  { verdi: "NYE_OPPLYSNINGER", label: "Nye opplysninger" },
  { verdi: "ANNET", label: "Annet" },
] as const;

export default function ArsakBehandling() {
  const erLesevisning = useErLesevisning();
  const iDag = new Date();

  const { behandlingId, årsakDataHentet } = useBehandlingContext();

  const {
    kravdato,
    årsak,
    beskrivelse,
    laster,
    feilmelding,
    erLagret,
    låst,
    oppdaterKravdato,
    oppdaterÅrsak,
    oppdaterBeskrivelse,
    lagre,
    settLåst,
    tilbakestill,
  } = useArsakBehandling(behandlingId);

  const { datepickerProps, inputProps, setSelected } = useDatepicker({
    defaultSelected: kravdato,
    toDate: iDag,
    onDateChange: oppdaterKravdato,
  });

  useMarkerStegFerdige(STEG_NAVN, erLagret);

  const erLåst = låst || erLesevisning;
  const kanLagre = kravdato !== undefined && årsak !== "";

  const håndterLagring = async () => {
    const suksess = await lagre();
    if (suksess) {
      settLåst(true);
    }
  };

  useEffect(() => {
    setSelected(kravdato);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [kravdato]);

  if (!årsakDataHentet) {
    return (
      <Box shadow="dialog" background="neutral-soft" padding="space-24" borderRadius="4">
        <Loader size="medium" />
      </Box>
    );
  }

  return (
    <VStack gap="space-24">
      <Box shadow="dialog" background="neutral-soft" padding="space-24" borderRadius="4">
        <VStack gap="space-24" style={{ position: "relative" }}>
          {låst && !erLesevisning && (
            <RedigerOgSlettKnapper onRediger={() => settLåst(false)} onSlett={tilbakestill} />
          )}

          <VStack gap="space-16">
            <div style={{ maxWidth: "24rem" }}>
              <DatePicker {...datepickerProps}>
                <DatePicker.Input {...inputProps} label="Kravdato" readOnly={erLåst} />
              </DatePicker>
            </div>

            <Select
              label="Årsak til behandling"
              onChange={(e) => oppdaterÅrsak(e.target.value as ÅrsakType)}
              value={årsak}
              disabled={erLåst}
              style={{ maxWidth: "24rem" }}
            >
              <option value="" disabled>
                Velg årsak
              </option>
              {ÅRSAK_ALTERNATIVER.map(({ verdi, label }) => (
                <option key={verdi} value={verdi}>
                  {label}
                </option>
              ))}
            </Select>
            <Textarea
              label="Beskrivelse av årsak (fylles ut ved behov)"
              onChange={(e) => oppdaterBeskrivelse(e.target.value)}
              value={beskrivelse}
              readOnly={erLåst}
            />
          </VStack>

          {!låst && (
            <div>
              <Button
                onClick={håndterLagring}
                disabled={!kanLagre || erLesevisning}
                loading={laster}
              >
                Lagre
              </Button>
            </div>
          )}

          {feilmelding && (
            <Alert variant="error" size="small">
              {feilmelding}
            </Alert>
          )}
        </VStack>
      </Box>

      <StegNavigering stegPath={STEG_PATH} nesteDisabled={!erLagret} />
    </VStack>
  );
}
