import React from "react";
import { InternalHeader, Spacer } from "@navikt/ds-react";
import type { Saksbehandler } from "~/server/types";
import { useNavigate, useRouteLoaderData } from "react-router";
import { Søkefelt } from "./Søkefelt";
import { SaksbehandlerMenu } from "./SaksbehandlerMenu";
import { useSøk } from "~/hooks/useSøk";
import { useOpprettFagsak } from "~/hooks/useOpprettFagsak";

export const Header: React.FC = () => {
  const navigate = useNavigate();
  const { søk, søkeresultat, søker, feilmelding, settSøk, tilbakestillSøk } = useSøk();
  const { opprettFagsak, oppretter, opprettFeilmelding } = useOpprettFagsak();

  const { saksbehandler } =
    useRouteLoaderData<{
      saksbehandler: Saksbehandler | null;
    }>("root") || {};

  const handleNavigate = (fagsakPersonId: string) => {
    navigate(`/person/${fagsakPersonId}/behandlingsoversikt`);
    tilbakestillSøk();
  };

  const handleOpprettFagsak = async () => {
    if (!søkeresultat) return;
    await opprettFagsak(søkeresultat);
    tilbakestillSøk();
  };

  return (
    <InternalHeader>
      <InternalHeader.Title as="a" href="/">
        Gjenlevende BS
      </InternalHeader.Title>
      <Spacer />

      <Søkefelt
        søk={søk}
        onSøkChange={settSøk}
        søker={søker || oppretter}
        feilmelding={feilmelding}
        søkeresultat={søkeresultat}
        onNavigate={handleNavigate}
        onOpprettFagsak={handleOpprettFagsak}
        onTilbakestillSøk={tilbakestillSøk}
        opprettFeilmelding={opprettFeilmelding}
      />

      <SaksbehandlerMenu saksbehandler={saksbehandler ?? null} />
    </InternalHeader>
  );
};

export default Header;
