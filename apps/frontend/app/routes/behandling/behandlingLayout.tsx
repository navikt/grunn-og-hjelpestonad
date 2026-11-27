import React, { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";
import { Outlet, useLocation, useNavigate, useParams, useRevalidator } from "react-router";
import { BehandlingContext, type ÅrsakState } from "~/fellesContext/BehandlingContext";
import { BehandlingFaner, type BehandlingSteg } from "~/komponenter/navbar/BehandlingFaner";
import { erFaneLåst, visningsnavnForGjeldendeSteg } from "~/komponenter/navbar/behandlingFaneRegel";
import { Side } from "~/komponenter/layout/Side";
import { HøyreMeny } from "~/komponenter/behandling/høyremeny/HøyreMeny";
import { hentBehandling, hentÅrsakBehandling } from "~/api/generated/sdk.gen";
import type { BehandlingResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { useLesevisningsContext } from "~/fellesContext/LesevisningsContext";
import { Box, Button, InlineMessage, VStack } from "@navikt/ds-react";
import { AnsvarligSaksbehandler } from "~/komponenter/behandling/høyremeny/AnsvarligSaksbehandler";
import { Totrinnskontroll } from "~/komponenter/behandling/høyremeny/Totrinnskontroll";
import { SidebarTabs } from "~/komponenter/behandling/høyremeny/SidebarTabs";
import { TildelOppgave } from "~/komponenter/behandling/høyremeny/TildelOppgave";
import { useHentAnsvarligSaksbehandler } from "~/hooks/useHentAnsvarligSaksbehandler";
import { useHentTotrinnskontrollStatus } from "~/hooks/useHentTotrinnskontrollStatus";
import { LocalAlertBehandlingFerdigstilt } from "./LocalAlertBehandlingFerdigstilt";
import { useHenleggBehandling } from "~/hooks/useHenleggBehandling";
import { HenleggBehandlingModal } from "~/komponenter/behandling/HenleggBehandlingModal";
import { lyttPåManglerTilgang } from "~/utils/manglerTilgangEvent";

const BEHANDLING_STEG_LISTE: BehandlingSteg[] = [
  {
    path: "arsak-behandling",
    navn: "Årsak behandling",
    behandlingssteg: "ÅRSAK",
  },
  {
    path: "vilkar",
    navn: "Inngangsvilkår",
    behandlingssteg: "INNGANGSVILKÅR",
  },
  {
    path: "sats-og-trygdetid",
    navn: "Sats og trygdetid",
    behandlingssteg: "SATS_OG_TRYGDETID",
  },
  {
    path: "simulering",
    navn: "Simulering",
    behandlingssteg: "VEDTAK_OG_BREV",
  },
  {
    path: "brev",
    navn: "Brev",
    behandlingssteg: "VEDTAK_OG_BREV",
  },
];

// Meldingen om omdirigering sendes i location.state, så den forsvinner ved neste navigering.
function hentLåstFaneMelding(state: unknown): string | undefined {
  if (
    typeof state === "object" &&
    state !== null &&
    "låstFaneMelding" in state &&
    typeof state.låstFaneMelding === "string"
  ) {
    return state.låstFaneMelding;
  }
  return undefined;
}

export default function BehandlingLayout() {
  const { behandlingId, fagsakPersonId } = useParams<{
    behandlingId: string;
    fagsakPersonId: string;
  }>();
  const [årsakState, settÅrsakState] = useState<ÅrsakState | undefined>(undefined);
  const [behandling, settBehandling] = useState<BehandlingResponse | undefined>(undefined);
  const [årsakDataHentet, settÅrsakDataHentet] = useState(false);
  const { settErLesevisning } = useLesevisningsContext();
  const revalidator = useRevalidator();
  const navigate = useNavigate();
  const location = useLocation();
  const henleggModalRef = useRef<HTMLDialogElement>(null);
  const { henleggBehandling, laster, henleggFeilmelding } = useHenleggBehandling();
  const låstFaneMelding = hentLåstFaneMelding(location.state);
  const aktivFane = BEHANDLING_STEG_LISTE.find((steg) =>
    location.pathname.endsWith(`/${steg.path}`)
  );

  const {
    ansvarligSaksbehandler,
    laster: lasterAnsvarligSaksbehandler,
    hentPåNytt: hentAnsvarligSaksbehandlerPåNytt,
  } = useHentAnsvarligSaksbehandler(behandlingId);

  const { totrinnskontrollStatus, hentPåNytt: hentTotrinnskontrollStatusPåNytt } =
    useHentTotrinnskontrollStatus(behandlingId);

  const oppdaterÅrsakState = useCallback((data: Partial<ÅrsakState>) => {
    settÅrsakState((prev) => ({ ...prev, ...data }) as ÅrsakState);
  }, []);

  useEffect(() => {
    if (!behandlingId || årsakDataHentet) return;

    const controller = new AbortController();
    const { signal } = controller;

    const hentData = async () => {
      try {
        const { data: behandlingData } = await hentBehandling({
          body: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;

        settBehandling(behandlingData);

        const { data: årsakData, response } = await hentÅrsakBehandling({
          path: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;

        const harÅrsak = response.status !== 204;

        // 204 betyr at årsak mangler. Da er årsakData tom og skal ikke leses.
        if (harÅrsak) {
          settÅrsakState({
            kravdato: new Date(årsakData.kravdato),
            årsak: årsakData.årsak || "",
            beskrivelse: årsakData.beskrivelse,
          });
        }

        settÅrsakDataHentet(true);
      } catch (feil: unknown) {
        if (signal.aborted) return;
        console.error(
          "Kunne ikke hente behandlingsdata:",
          feilmeldingFra(feil, "Kunne ikke hente behandlingsdata")
        );
        settÅrsakDataHentet(true);
      }
    };

    hentData();

    return () => controller.abort();
  }, [behandlingId, årsakDataHentet]);

  useEffect(() => {
    if (!behandling || !ansvarligSaksbehandler) return;

    const statusLåser =
      behandling.status === "IVERKSETTER_VEDTAK" ||
      behandling.status === "FERDIGSTILT" ||
      behandling.status === "FATTER_VEDTAK";

    const erIkkeAnsvarligSaksbehandler = ansvarligSaksbehandler.rolle !== "INNLOGGET_SAKSBEHANDLER";

    settErLesevisning(statusLåser || erIkkeAnsvarligSaksbehandler);
  }, [behandling, ansvarligSaksbehandler, settErLesevisning]);

  useEffect(() => {
    return lyttPåManglerTilgang(() => {
      hentAnsvarligSaksbehandlerPåNytt();
    });
  }, [hentAnsvarligSaksbehandlerPåNytt]);

  const hentBehandlingPåNytt = useCallback(async () => {
    if (!behandlingId) return;

    try {
      const { data } = await hentBehandling({
        body: { behandlingId },
        throwOnError: true,
      });

      settBehandling(data);
    } catch (feil: unknown) {
      console.error(
        "Kunne ikke hente behandling:",
        feilmeldingFra(feil, "Kunne ikke hente behandling")
      );
    }
  }, [behandlingId]);

  // Behandlingen kan ha endret seg i en annen fane eller av en annen saksbehandler mens denne fanen var skjult.
  useEffect(() => {
    const hentBehandlingNårFanenBlirSynlig = () => {
      if (document.visibilityState === "visible") void hentBehandlingPåNytt();
    };
    document.addEventListener("visibilitychange", hentBehandlingNårFanenBlirSynlig);
    return () => document.removeEventListener("visibilitychange", hentBehandlingNårFanenBlirSynlig);
  }, [hentBehandlingPåNytt]);

  // En låst fane i URL-en sender brukeren til fanen for gjeldende steg, med en melding.
  useEffect(() => {
    if (!behandling || !aktivFane || !erFaneLåst(aktivFane.behandlingssteg, behandling)) return;

    const fanenSomHørerTilGjeldendeSteg = BEHANDLING_STEG_LISTE.find(
      (steg) => steg.behandlingssteg === behandling.gjeldendeSteg
    );
    if (!fanenSomHørerTilGjeldendeSteg) return;

    navigate(fanenSomHørerTilGjeldendeSteg.path, {
      replace: true,
      state: {
        låstFaneMelding: `Fullfør «${visningsnavnForGjeldendeSteg(behandling.gjeldendeSteg)}» før du går videre.`,
      },
    });
  }, [behandling, aktivFane, navigate]);

  const revaliderBehandling = useCallback(() => {
    settÅrsakDataHentet(false);
    revalidator.revalidate();
  }, [revalidator]);

  const hentÅrsakData = useCallback(async () => {
    if (!behandlingId) return;

    try {
      const { data, response } = await hentÅrsakBehandling({
        path: { behandlingId },
        throwOnError: true,
      });

      if (response.status === 204) return;

      settÅrsakState({
        kravdato: new Date(data.kravdato),
        årsak: data.årsak || "",
        beskrivelse: data.beskrivelse,
      });
    } catch (feil: unknown) {
      console.error(
        "Kunne ikke hente årsak data:",
        feilmeldingFra(feil, "Kunne ikke hente årsak data")
      );
    }
  }, [behandlingId]);

  const erBehandlingFerdigstilt = behandling?.status === "FERDIGSTILT";
  const erBehandlingIverksetter = behandling?.status === "IVERKSETTER_VEDTAK";
  const kanHenlegges = behandling && !erBehandlingFerdigstilt && !erBehandlingIverksetter;

  // Portal-målet rendres av Personheader utenfor dette komponenttreet, og finnes derfor
  // først etter at klienten har montert. Elementet byttes aldri ut, så vi trenger ikke abonnere.
  const personheaderActions = useSyncExternalStore(
    () => () => {},
    () => document.getElementById("personheader-actions"),
    () => null
  );

  const håndterHenlegg = async () => {
    if (!behandlingId) return;
    const suksess = await henleggBehandling(behandlingId);
    if (suksess) {
      henleggModalRef.current?.close();
      navigate(`/person/${fagsakPersonId}/behandlingsoversikt`);
    }
  };

  if (!behandlingId) {
    return <div>Mangler behandling id</div>;
  }

  return (
    <BehandlingContext.Provider
      value={{
        behandlingId,
        stegListe: BEHANDLING_STEG_LISTE,
        behandling,
        årsakState,
        oppdaterÅrsakState,
        hentÅrsakData,
        årsakDataHentet,
        hentBehandlingPåNytt,
        revaliderBehandling,
        ansvarligSaksbehandler,
        lasterAnsvarligSaksbehandler,
        hentAnsvarligSaksbehandlerPåNytt,
        totrinnskontrollStatus,
        hentTotrinnskontrollStatusPåNytt,
      }}
    >
      <Box style={{ display: "flex", flexDirection: "column", flex: 1, minHeight: 0 }}>
        {kanHenlegges &&
          personheaderActions &&
          createPortal(
            <Button
              variant="danger"
              size="small"
              onClick={() => henleggModalRef.current?.showModal()}
              disabled={true} // TODO: Skal feature toggles.
            >
              Henlegg
            </Button>,
            personheaderActions
          )}
        <HenleggBehandlingModal
          modalRef={henleggModalRef}
          laster={laster}
          feilmelding={henleggFeilmelding}
          onHenlegg={håndterHenlegg}
        />
        <BehandlingFaner steg={BEHANDLING_STEG_LISTE} behandling={behandling} />
        <Box style={{ display: "flex", flex: 1, minHeight: 0 }}>
          <Box style={{ flex: 1, overflowY: "auto", minWidth: 0 }}>
            <Side>
              <VStack gap="space-24">
                {låstFaneMelding && (
                  <InlineMessage status="info" size="small" role="status">
                    {låstFaneMelding}
                  </InlineMessage>
                )}
                <Outlet />
              </VStack>
            </Side>
          </Box>
          <HøyreMeny>
            <TildelOppgave />

            {erBehandlingFerdigstilt && <LocalAlertBehandlingFerdigstilt behandling={behandling} />}

            <Totrinnskontroll />

            <AnsvarligSaksbehandler />
            <SidebarTabs />
          </HøyreMeny>
        </Box>
      </Box>
    </BehandlingContext.Provider>
  );
}
