import React, { useEffect, useRef } from "react";
import { ErrorSummary, LocalAlert } from "@navikt/ds-react";
import type { InngangsvilkårBlokkertResponse } from "~/api/generated/types.gen";
import type { FullførInngangsvilkårFeil } from "~/komponenter/behandling/vilkår/felles/useFullførInngangsvilkår";
import type { Valideringsnøkkel } from "~/komponenter/behandling/vilkår/felles/valideringsfeilFra";

const hrefPåVilkårssiden: Record<Valideringsnøkkel, string> = {
  MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN: "#vilkar-medlemskap",
  DIAGNOSE: "#vilkar-diagnose",
  NØDVENDIGE_EKSTRAUTGIFTER: "#vilkar-ekstrautgifter",
  TRYGDETID: "#trygdetid",
};

const tittelForBlokkertFeil: Record<InngangsvilkårBlokkertResponse["feilkode"], string> = {
  FALSK_IDENTITET: "Behandlingen kan ikke gå videre",
};

export function FullførInngangsvilkårFeilvisning({ feil }: { feil: FullførInngangsvilkårFeil }) {
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (feil.type === "validering") {
      feiloppsummeringRef.current?.focus();
    }
  }, [feil]);

  switch (feil.type) {
    case "generell":
      return (
        <LocalAlert status="error">
          <LocalAlert.Content>{feil.melding}</LocalAlert.Content>
        </LocalAlert>
      );
    case "validering":
      return (
        <ErrorSummary
          ref={feiloppsummeringRef}
          heading="Du må vurdere disse vilkårene før du kan gå videre:"
        >
          {feil.feil.map((valideringsfeil) => (
            <ErrorSummary.Item
              key={valideringsfeil.nøkkel}
              href={hrefPåVilkårssiden[valideringsfeil.nøkkel]}
            >
              {valideringsfeil.melding}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      );
    case "blokkert":
      return (
        <LocalAlert status="error">
          <LocalAlert.Header>
            <LocalAlert.Title>{tittelForBlokkertFeil[feil.feil.feilkode]}</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>{feil.feil.melding}</LocalAlert.Content>
        </LocalAlert>
      );
    default: {
      const ukjentFeiltype: never = feil;
      throw new Error(`Ukjent feiltype for inngangsvilkår: ${ukjentFeiltype}`);
    }
  }
}
