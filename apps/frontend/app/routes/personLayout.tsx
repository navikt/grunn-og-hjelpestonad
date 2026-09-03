import React, { useState } from "react";
import { Outlet, useParams, useMatch } from "react-router";
import { Alert, Loader, VStack } from "@navikt/ds-react";
import { Navbar } from "~/komponenter/navbar/Navbar";
import { Side } from "~/komponenter/layout/Side";
import Personheader from "~/komponenter/personheader/Personheader";
import { PersonContext } from "~/contexts/PersonContext";
import { LesevisningsContext } from "~/contexts/LesevisningsContext";
import { useHentPdlNavn } from "~/hooks/useHentPerson";
import { useFagsak } from "~/hooks/useFagsak";

export default function PersonLayout() {
  const { fagsakPersonId } = useParams<{ fagsakPersonId: string }>();
  const erPåBehandling = useMatch("/person/:fagsakPersonId/behandling/:behandlingId/*");
  const { fagsak, melding: fagsakMelding, laster: lasterFagsak } = useFagsak(fagsakPersonId);

  const personident = fagsak?.personident;
  const { person, melding, laster: lasterPerson } = useHentPdlNavn(fagsakPersonId);

  const laster = lasterFagsak || lasterPerson;
  const [erLesevisning, settErLesevisning] = useState(false);

  if (laster) {
    return (
      <VStack gap="space-6" align="center" style={{ padding: "2rem" }}>
        <Loader size="large" title="Henter personinformasjon..." />
      </VStack>
    );
  }

  if (!fagsakPersonId || !personident) {
    return (
      <VStack gap="space-4" style={{ padding: "2rem" }}>
        <Alert variant="error">
          Kunne ikke hente personinformasjon: {melding || "Mangler data"}
          {fagsakMelding && <p>{fagsakMelding}</p>}
        </Alert>
      </VStack>
    );
  }

  return (
    <PersonContext.Provider
      value={{
        person: person,
        personident: personident,
        fagsakPersonId: fagsakPersonId,
        fagsak: fagsak,
        fagsakId: fagsak?.id,
        laster: lasterPerson,
      }}
    >
      <LesevisningsContext.Provider value={{ erLesevisning, settErLesevisning }}>
        <Personheader />
        {!erPåBehandling ? (
          <>
            <Navbar />
            <Side>
              <Outlet />
            </Side>
          </>
        ) : (
          <Outlet />
        )}
      </LesevisningsContext.Provider>
    </PersonContext.Provider>
  );
}
