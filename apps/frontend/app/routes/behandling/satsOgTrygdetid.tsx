import React, { useRef, useState } from "react";
import type { Route } from "./+types/satsOgTrygdetid";
import { Box, Loader, Select, VStack } from "@navikt/ds-react";
import type { ResultatType } from "~/komponenter/behandling/vedtak/vedtak";
import { InnvilgeVedtak } from "~/komponenter/behandling/vedtak/InnvilgeVedtak";
import { useHentVedtak } from "~/hooks/useHentVedtak";
import { useParams } from "react-router";
import { AvslåVedtak } from "~/komponenter/behandling/vedtak/AvslåVedtak";
import { OpphørVedtak } from "~/komponenter/behandling/vedtak/OpphørVedtak";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { RedigerOgSlettKnapper } from "~/komponenter/behandling/RedigerOgSlettKnapper";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Sats og trygdetid" }];
}

const STEG_PATH: StegPath = "sats-og-trygdetid";

export default function SatsOgTrygdetid() {
  const [vedtaksresultat, settVedtaksResultat] = useState<ResultatType | undefined>(undefined);
  const [låst, settLåst] = useState(false);
  const [erLagret, settErLagret] = useState(false);
  const [erSlettet, settErSlettet] = useState(false);
  const { behandlingId } = useParams<{ behandlingId: string }>();
  const { vedtak, laster: lasterVedtak } = useHentVedtak(behandlingId);
  const erLesevisning = useErLesevisning();

  const behandling = useBehandlingContext().behandling;
  const erFørstegangsBehandling = Boolean(behandling && !behandling.forrigeBehandlingId);

  const harSjekketInitiellLås = useRef(false);

  React.useEffect(() => {
    if (vedtak?.resultatType && !harSjekketInitiellLås.current) {
      harSjekketInitiellLås.current = true;
      settVedtaksResultat(vedtak.resultatType);
      settLåst(true);
      settErLagret(true);
    }
  }, [vedtak]);

  const handleVedtaksresultatEndring = (value: string) => {
    const resultat = value === "" ? undefined : (value as ResultatType);
    settVedtaksResultat(resultat);
  };

  const handleLagreSuksess = () => {
    settLåst(true);
    settErLagret(true);
    settErSlettet(false);
  };

  const handleSlett = () => {
    settVedtaksResultat(undefined);
    settLåst(false);
    settErLagret(false);
    settErSlettet(true);
  };

  if (lasterVedtak) {
    return (
      <Box shadow="dialog" background="neutral-soft" padding="space-24" borderRadius="4">
        <Loader size="medium" />
      </Box>
    );
  }

  return (
    <VStack gap="space-24">
      <Box shadow="dialog" background="neutral-soft" padding="space-24" borderRadius="4">
        <VStack gap="space-12" style={{ position: "relative" }}>
          {låst && !erLesevisning && (
            <RedigerOgSlettKnapper onRediger={() => settLåst(false)} onSlett={handleSlett} />
          )}
          <Select
            label={"Vedtaksresultat"}
            value={vedtaksresultat || ""}
            onChange={(e) => handleVedtaksresultatEndring(e.target.value)}
            disabled={erLesevisning || låst}
            style={{ maxWidth: "24rem" }}
          >
            <option value="">Velg</option>
            <option value="AVSLÅTT">Avslå</option>
            <option value="OPPHØR" disabled={erFørstegangsBehandling}>
              Opphør
            </option>
          </Select>
          {vedtaksresultat === "INNVILGET" && (
            <InnvilgeVedtak
              lagretVedtak={erSlettet ? null : vedtak}
              erLesevisning={erLesevisning}
              låst={låst}
              onLagreSuksess={handleLagreSuksess}
            />
          )}
          {vedtaksresultat === "AVSLÅTT" && (
            <AvslåVedtak
              lagretVedtak={erSlettet ? null : vedtak}
              erLesevisning={erLesevisning}
              låst={låst}
              onLagreSuksess={handleLagreSuksess}
            />
          )}
          {vedtaksresultat === "OPPHØR" && (
            <OpphørVedtak
              lagretVedtak={erSlettet ? null : vedtak}
              erLesevisning={erLesevisning}
              låst={låst}
              onLagreSuksess={handleLagreSuksess}
            />
          )}
        </VStack>
      </Box>

      <StegNavigering stegPath={STEG_PATH} nesteDisabled={!erLagret} />
    </VStack>
  );
}
