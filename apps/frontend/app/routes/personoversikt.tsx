import React, { useState } from "react";
import { VStack, BodyShort, Button, Alert } from "@navikt/ds-react";
import type { Route } from "./+types/personoversikt";
import { usePersonContext } from "~/contexts/PersonContext";
import { formaterNavn } from "~/utils/utils";
import { hentEtterlatteSakIdMedPersonident } from "~/api/etterlatteBehandling";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

export function meta(_: Route.MetaArgs) {
  return [{ title: `Personoversikt` }];
}

const GJENNY_URL_DEV = "https://etterlatte-saksbehandling.intern.dev.nav.no";

export default function PersonOversikt() {
  const { personident, person } = usePersonContext();
  const visningsNavn = person?.navn ? formaterNavn(person.navn) : "Navn ikke tilgjengelig";
  const [laster, settLaster] = useState(false);
  const [feilmelding, settFeilmelding] = useState("");

  const åpnePersonopplysningerIGjenny = async () => {
    if (!personident) return;

    settLaster(true);
    settFeilmelding("");
    const { data: sak, melding } = await hentEtterlatteSakIdMedPersonident({ fnr: personident });

    if (!sak) {
      settFeilmelding(`Klarte ikke hente etterlatte-sak: ${melding}`);
      settLaster(false);
      return;
    }

    const gjennyUrl = GJENNY_URL_DEV;
    window.open(
      `${gjennyUrl}/person/${sak.sakId}?fane=PERSONOPPLYSNINGER`,
      "_blank",
      "noopener,noreferrer"
    );
    settLaster(false);
  };

  return (
    <VStack gap="space-4">
      <BodyShort size="large" weight="semibold">
        {visningsNavn}
      </BodyShort>
      <BodyShort>Personident: {personident || "Ikke tilgjengelig"}</BodyShort>

      <div>
        <Button
          onClick={åpnePersonopplysningerIGjenny}
          size="small"
          icon={<ExternalLinkIcon title="åpne lenke til personopplysninger i gjenny" />}
          loading={laster}
          disabled={laster}
        >
          Åpne Gjenny
        </Button>
        {feilmelding && (
          <Alert variant="error" size="small">
            {feilmelding}
          </Alert>
        )}
      </div>
    </VStack>
  );
}
