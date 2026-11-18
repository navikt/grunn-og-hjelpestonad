import React, { useEffect, useRef, useState } from "react";
import type { Route } from "./+types/satsOgTrygdetid";
import { Accordion, BodyShort, ErrorSummary, LocalAlert, VStack } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { StegNavigering } from "~/komponenter/behandling/StegNavigering";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { PerioderMedOppfylteInngangsvilkårTidslinje } from "~/komponenter/behandling/satsOgTrygdetid/PerioderMedOppfylteInngangsvilkårTidslinje";
import { EkstrautgifterVilkår } from "~/komponenter/behandling/satsOgTrygdetid/ekstrautgifter/EkstrautgifterVilkår";
import { TrygdetidSeksjon } from "~/komponenter/behandling/satsOgTrygdetid/trygdetid/TrygdetidSeksjon";
import { fullførSatsOgTrygdetid } from "~/api/generated/sdk.gen";
import { zSatsOgTrygdetidFeilResponse } from "~/api/generated/zod.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { useApiKall } from "~/komponenter/behandling/vilkår/felles/useApiKall";

export function meta(_: Route.MetaArgs) {
  return [{ title: "Sats og trygdetid" }];
}

const STEG_PATH: StegPath = "sats-og-trygdetid";

export default function SatsOgTrygdetid() {
  const { behandlingId, hentBehandlingPåNytt } = useBehandlingContext();
  const navigate = useNavigate();
  const { navigerTilNeste } = useStegNavigering(STEG_PATH);
  const erLesevisning = useErLesevisning();
  const { kall: fullførSteg, laster: fullfører } =
    useApiKall<Parameters<typeof fullførSatsOgTrygdetid>[0]>(fullførSatsOgTrygdetid);
  const [valideringsfeil, settValideringsfeil] = useState<{ nøkkel: string; melding: string }[]>(
    []
  );
  const [feilmelding, settFeilmelding] = useState<string | undefined>();
  const [trygdetidFeilKlikkAntall, settTrygdetidFeilKlikkAntall] = useState(0);
  const [ekstrautgifterFeilKlikkAntall, settEkstrautgifterFeilKlikkAntall] = useState(0);
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (valideringsfeil.length > 0) feiloppsummeringRef.current?.focus();
  }, [valideringsfeil]);

  const fullfør = () => {
    settValideringsfeil([]);
    settFeilmelding(undefined);
    fullførSteg(
      { path: { behandlingId }, throwOnError: true },
      {
        onSuccess: async () => {
          // Fullføringen flytter nåværendeSteg, så behandlingen hentes på nytt før navigering.
          await hentBehandlingPåNytt();
          navigerTilNeste();
        },
        onError: (error: unknown) => {
          const valideringsfeilResponse = zSatsOgTrygdetidFeilResponse.safeParse(error);
          if (valideringsfeilResponse.success) {
            settValideringsfeil(valideringsfeilResponse.data.feil);
          } else {
            settFeilmelding(feilmeldingFra(error, "Kunne ikke beregne tilkjent ytelse."));
          }
        },
      }
    );
  };

  return (
    <VStack gap="space-24">
      {behandlingId && <PerioderMedOppfylteInngangsvilkårTidslinje behandlingId={behandlingId} />}

      {behandlingId && (
        <Accordion>
          <EkstrautgifterVilkår
            behandlingId={behandlingId}
            åpneVedFeil={ekstrautgifterFeilKlikkAntall}
          />
          <TrygdetidSeksjon behandlingId={behandlingId} åpneVedFeil={trygdetidFeilKlikkAntall} />
        </Accordion>
      )}

      {feilmelding && (
        <LocalAlert status="error">
          <LocalAlert.Content>
            <BodyShort>{feilmelding}</BodyShort>
            <BodyShort>Prøv igjen ved å velge Neste.</BodyShort>
          </LocalAlert.Content>
        </LocalAlert>
      )}

      {valideringsfeil.length > 0 && (
        <ErrorSummary
          ref={feiloppsummeringRef}
          heading="Du må rette disse feilene før du kan fortsette"
        >
          {valideringsfeil.map((valideringsfeil, indeks) => (
            <ErrorSummary.Item
              key={`${valideringsfeil.nøkkel}-${indeks}`}
              href={
                valideringsfeil.nøkkel === "vilkår" || valideringsfeil.nøkkel === "inngangsvilkår"
                  ? "../vilkar"
                  : valideringsfeil.nøkkel === "trygdetid"
                    ? "#trygdetid"
                    : valideringsfeil.nøkkel === "ekstrautgifter"
                      ? "#vilkar-ekstrautgifter"
                      : undefined
              }
              onClick={(event) => {
                if (
                  valideringsfeil.nøkkel === "vilkår" ||
                  valideringsfeil.nøkkel === "inngangsvilkår"
                ) {
                  event.preventDefault();
                  navigate("../vilkar", { relative: "path" });
                } else if (valideringsfeil.nøkkel === "trygdetid") {
                  settTrygdetidFeilKlikkAntall((antall) => antall + 1);
                } else if (valideringsfeil.nøkkel === "ekstrautgifter") {
                  settEkstrautgifterFeilKlikkAntall((antall) => antall + 1);
                }
              }}
            >
              {valideringsfeil.melding}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}
      <StegNavigering
        stegPath={STEG_PATH}
        onNeste={erLesevisning ? navigerTilNeste : fullfør}
        nesteLaster={fullfører}
        nesteDisabled={fullfører || !behandlingId}
      />
    </VStack>
  );
}
