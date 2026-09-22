import { index, route, type RouteConfig } from "@react-router/dev/routes";

export default [
  index("routes/landingsside.tsx"),
  route("/person/:fagsakPersonId", "routes/personLayout.tsx", [
    route("behandlingsoversikt", "routes/behandlingsoversikt.tsx"),
    route("behandling/:behandlingId", "routes/behandling/behandlingLayout.tsx", [
      route("arsak-behandling", "routes/behandling/årsakBehandling.tsx"),
      route("vilkar", "routes/behandling/vilkår.tsx"),
      route("sats-og-trygdetid", "routes/behandling/satsOgTrygdetid.tsx"),
      route("simulering", "routes/behandling/simulering.tsx"),
      route("brev", "routes/behandling/brev.tsx"),
    ]),
    route("personoversikt", "routes/personoversikt.tsx"),
    route("infotrygd-historikk", "routes/infotrygdHistorikk.tsx"),
    route("dokumentoversikt", "routes/dokumentoversikt.tsx"),
  ]),
] satisfies RouteConfig;
