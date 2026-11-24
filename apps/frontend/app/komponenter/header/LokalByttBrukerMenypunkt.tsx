import React from "react";
import { ArrowsCirclepathIcon } from "@navikt/aksel-icons";
import { Spacer } from "@navikt/ds-react";
import { useLocation } from "react-router";

interface LokalByttBrukerMenypunktProps {
  navIdent?: string;
  className: string;
}

export const LokalByttBrukerMenypunkt: React.FC<LokalByttBrukerMenypunktProps> = ({
  navIdent,
  className,
}) => {
  const location = useLocation();
  const nåværendeSti = `${location.pathname}${location.search}`;

  const erInnloggetSomBeslutter = navIdent === "B123456";
  const nesteBruker = erInnloggetSomBeslutter ? "saksbehandler" : "beslutter";
  const byttBrukerTekst = erInnloggetSomBeslutter
    ? "Bytt til saksbehandler (A123456)"
    : "Bytt til beslutter (B123456)";
  const byttBrukerLenke = `/oauth2/login?bruker=${nesteBruker}&redirect=${encodeURIComponent(nåværendeSti)}`;

  return (
    <li>
      <a href={byttBrukerLenke} className={className}>
        {byttBrukerTekst} <Spacer /> <ArrowsCirclepathIcon aria-hidden />
      </a>
    </li>
  );
};
