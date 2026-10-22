import React, { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";
import { Outlet, useNavigate, useParams, useRevalidator } from "react-router";
import { BehandlingContext, type ÅrsakState } from "~/fellesContext/BehandlingContext";
import {
  BehandlingFaner,
  type BehandlingSteg,
  type Steg,
} from "~/komponenter/navbar/BehandlingFaner";
import { Side } from "~/komponenter/layout/Side";
import { HøyreMeny } from "~/komponenter/behandling/høyremeny/HøyreMeny";
import { hentBehandling, hentÅrsakBehandling } from "~/api/generated/sdk.gen";
import type { BehandlingResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { useLesevisningsContext } from "~/fellesContext/LesevisningsContext";
import { Box, Button } from "@navikt/ds-react";
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
    kanStarte: () => true,
  },
  {
    path: "vilkar",
    navn: "Vilkår",
    kanStarte: (ferdigeSteg) => ferdigeSteg.includes("Årsak behandling"),
  },
  {
    path: "sats-og-trygdetid",
    navn: "Sats og trygdetid",
  },
  {
    path: "simulering",
    navn: "Simulering",
  },
  {
    path: "brev",
    navn: "Brev",
  },
];

export default function BehandlingLayout() {
  const { behandlingId, fagsakPersonId } = useParams<{
    behandlingId: string;
    fagsakPersonId: string;
  }>();
  const [ferdigeSteg, settFerdigeSteg] = useState<Steg[]>([]);
  const [årsakState, settÅrsakState] = useState<ÅrsakState | undefined>(undefined);
  const [behandling, settBehandling] = useState<BehandlingResponse | undefined>(undefined);
  const [årsakDataHentet, settÅrsakDataHentet] = useState(false);
  const { settErLesevisning } = useLesevisningsContext();
  const revalidator = useRevalidator();
  const navigate = useNavigate();
  const henleggModalRef = useRef<HTMLDialogElement>(null);
  const { henleggBehandling, laster, henleggFeilmelding } = useHenleggBehandling();

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

        if (harÅrsak) {
          settÅrsakState({
            kravdato: new Date(årsakData.kravdato),
            årsak: årsakData.årsak || "",
            beskrivelse: årsakData.beskrivelse,
          });
        }

        // 204 betyr at årsak mangler. Da er årsakData tom og skal ikke leses.
        const erÅrsakFerdig = harÅrsak && Boolean(årsakData.kravdato && årsakData.årsak);
        settFerdigeSteg(erÅrsakFerdig ? ["Årsak behandling"] : []);
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
        ferdigeSteg,
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
        <BehandlingFaner steg={BEHANDLING_STEG_LISTE} ferdigeSteg={ferdigeSteg} />
        <Box style={{ display: "flex", flex: 1, minHeight: 0 }}>
          <Box style={{ flex: 1, overflowY: "auto", minWidth: 0 }}>
            <Side>
              <Outlet />
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
