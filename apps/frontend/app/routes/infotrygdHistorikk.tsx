import React from "react";
import type { Route } from "./+types/infotrygdHistorikk";
import { usePersonContext } from "~/fellesContext/PersonContext";
import { InfotrygdOversikt } from "~/komponenter/infotrygd/InfotrygdOversikt";

export function meta(_: Route.MetaArgs) {
  return [
    { title: "Historikk i Infotrygd" },
    {
      name: "description",
      content: "Oversikt over historikk fra Infotrygd",
    },
  ];
}

export default function InfotrygdHistorikk(_: Route.ComponentProps) {
  const { personident } = usePersonContext();

  return <InfotrygdOversikt personident={personident} />;
}
