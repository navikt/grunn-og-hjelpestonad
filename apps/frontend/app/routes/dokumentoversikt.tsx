import { useParams } from "react-router";
import { LocalAlert, Loader, VStack } from "@navikt/ds-react";
import React from "react";
import { useHentDokumenter } from "~/hooks/useHentDokumenter";
import { Dokumentliste } from "~/komponenter/dokumentoversikt/Dokumentliste";

export default function Dokumentoversikt() {
  const { fagsakPersonId } = useParams<{ fagsakPersonId: string }>();
  const { dokumenter, laster, melding } = useHentDokumenter(fagsakPersonId);

  if (laster) {
    return (
      <div>
        Henter dokumenter...
        <Loader title="Laster..." />
      </div>
    );
  }

  if (melding || !dokumenter || !fagsakPersonId) {
    return (
      <VStack gap="space-4" style={{ padding: "2rem" }}>
        <LocalAlert status="error">
          <LocalAlert.Content>Kunne ikke hente dokumenter: {melding ?? "Mangler data"}</LocalAlert.Content>
        </LocalAlert>
      </VStack>
    );
  }
  return (
    <VStack>
      <Dokumentliste dokumenter={dokumenter} medScroll fagsakPersonId={fagsakPersonId} />
    </VStack>
  );
}
