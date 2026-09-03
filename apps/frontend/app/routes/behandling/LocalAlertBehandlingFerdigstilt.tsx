import { LocalAlert } from "@navikt/ds-react";
import React from "react";
import { InfoRad } from "~/komponenter/behandling/høyremeny/InfoRad";
import type { Behandling } from "~/types/behandling";
import { formatterEnumVerdi } from "~/utils/utils";

export const LocalAlertBehandlingFerdigstilt: React.FC<{
  behandling: Behandling;
}> = ({ behandling }) => {
  const { status, resultat } = behandling;

  return (
    <LocalAlert status="success">
      <LocalAlert.Header>
        <LocalAlert.Title>Behandling er ferdigstilt</LocalAlert.Title>
      </LocalAlert.Header>
      <LocalAlert.Content>
        <InfoRad label={"Status"} verdi={formatterEnumVerdi(status)} />
        <InfoRad label={"Resultat"} verdi={formatterEnumVerdi(resultat)} />
      </LocalAlert.Content>
    </LocalAlert>
  );
};
