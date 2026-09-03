import React, { useCallback, useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Outlet, useNavigate, useParams, useRevalidator } from "react-router";
import { BehandlingContext, type ÅrsakState } from "~/contexts/BehandlingContext";
import {
  BehandlingFaner,
  type BehandlingSteg,
  type Steg,
} from "~/komponenter/navbar/BehandlingFaner";
import { Side } from "~/komponenter/layout/Side";
import { HøyreMeny } from "~/komponenter/behandling/høyremeny/HøyreMeny";
import { apiCall, type ApiResponse } from "~/api/backend";
import type { ÅrsakBehandlingResponse } from "~/hooks/useÅrsakBehandling";
import type { VilkårVurderingResponse } from "~/hooks/useVilkårVurdering";
import type { Behandling } from "~/types/behandling";
import { useLesevisningsContext } from "~/contexts/LesevisningsContext";
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
    path: "vedtak-og-beregning",
    navn: "Vedtak og beregning",
    kanStarte: (ferdigeSteg) => ferdigeSteg.includes("Vilkår"),
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
  const [behandling, settBehandling] = useState<Behandling | undefined>(undefined);
  const [årsakDataHentet, settÅrsakDataHentet] = useState(false);
  const { settErLesevisning } = useLesevisningsContext();
  const revalidator = useRevalidator();
  const navigate = useNavigate();
  const henleggModalRef = useRef<HTMLDialogElement>(null);
  const { henleggBehandling, laster, henleggFeilmelding } = useHenleggBehandling();
  const [personheaderActions, settPersonheaderActions] = useState<HTMLElement | null>(null);

  const {
    ansvarligSaksbehandler,
    laster: lasterAnsvarligSaksbehandler,
    hentPåNytt: hentAnsvarligSaksbehandlerPåNytt,
  } = useHentAnsvarligSaksbehandler(behandlingId);

  const { totrinnskontrollStatus, hentPåNytt: hentTotrinnskontrollStatusPåNytt } =
    useHentTotrinnskontrollStatus(behandlingId);

  const markerStegSomFerdig = useCallback((steg: Steg) => {
    settFerdigeSteg((prev) => (prev.includes(steg) ? prev : [...prev, steg]));
  }, []);

  const oppdaterÅrsakState = useCallback((data: Partial<ÅrsakState>) => {
    settÅrsakState((prev) => ({ ...prev, ...data }) as ÅrsakState);
  }, []);

  useEffect(() => {
    if (!behandlingId || årsakDataHentet) return;

    const hentData = async () => {
      try {
        const behandlingResponse: ApiResponse<Behandling> = await apiCall(`/behandling/hent`, {
          method: "POST",
          body: JSON.stringify({ behandlingId }),
        });

        if (behandlingResponse.data) {
          settBehandling(behandlingResponse.data);
        }

        const årsakResponse: ApiResponse<ÅrsakBehandlingResponse> = await apiCall(
          `/arsak/${behandlingId}`
        );

        const vilkårResponse: ApiResponse<VilkårVurderingResponse[]> = await apiCall(
          `/vilkar/${behandlingId}`
        );

        const initialFerdigeSteg: Steg[] = [];

        if (årsakResponse.data) {
          const data = årsakResponse.data;
          settÅrsakState({
            kravdato: new Date(data.kravdato),
            årsak: data.årsak || "",
            beskrivelse: data.beskrivelse,
          });

          const erÅrsakFerdig = data.kravdato && data.årsak;
          if (erÅrsakFerdig) {
            initialFerdigeSteg.push("Årsak behandling");
          }
        }

        if (vilkårResponse.data && vilkårResponse.data.length === 5) {
          const alleVilkårFerdige = vilkårResponse.data.every(
            (v) => v.vurdering && v.begrunnelse && v.begrunnelse.trim() !== ""
          );
          if (alleVilkårFerdige) {
            initialFerdigeSteg.push("Vilkår");
          }
        }

        settFerdigeSteg(initialFerdigeSteg);
        settÅrsakDataHentet(true);
      } catch (error) {
        console.error("Kunne ikke hente behandlingsdata:", error);
        settÅrsakDataHentet(true);
      }
    };

    hentData();
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
      const response: ApiResponse<Behandling> = await apiCall(`/behandling/hent`, {
        method: "POST",
        body: JSON.stringify({ behandlingId }),
      });

      if (response.data) {
        settBehandling(response.data);
      }
    } catch (error) {
      console.error("Kunne ikke hente behandling:", error);
    }
  }, [behandlingId]);

  const revaliderBehandling = useCallback(() => {
    settÅrsakDataHentet(false);
    revalidator.revalidate();
  }, [revalidator]);

  const hentÅrsakData = useCallback(async () => {
    if (!behandlingId) return;

    try {
      const response: ApiResponse<ÅrsakBehandlingResponse> = await apiCall(
        `/arsak/${behandlingId}`
      );

      if (response.data) {
        settÅrsakState({
          kravdato: new Date(response.data.kravdato),
          årsak: response.data.årsak || "",
          beskrivelse: response.data.beskrivelse,
        });
      }
    } catch (error) {
      console.error("Kunne ikke hente årsak data:", error);
    }
  }, [behandlingId]);

  const erBehandlingFerdigstilt = behandling?.status === "FERDIGSTILT";
  const erBehandlingIverksetter = behandling?.status === "IVERKSETTER_VEDTAK";
  const kanHenlegges = behandling && !erBehandlingFerdigstilt && !erBehandlingIverksetter;

  useEffect(() => {
    settPersonheaderActions(document.getElementById("personheader-actions"));
  }, []);

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
        markerStegSomFerdig,
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
