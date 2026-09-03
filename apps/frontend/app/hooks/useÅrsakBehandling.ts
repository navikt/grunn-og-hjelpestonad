import { useState, useEffect, useCallback, useRef } from "react";
import type { ÅrsakType } from "~/types/årsak";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import { apiCall, type ApiResponse } from "~/api/backend";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";

export interface ÅrsakBehandlingRequest {
  kravdato: string;
  årsak: ÅrsakType;
  beskrivelse: string;
}

export interface ÅrsakBehandlingResponse {
  kravdato: string;
  årsak: ÅrsakType;
  beskrivelse: string;
}

interface UseÅrsakBehandling {
  kravdato: Date | undefined;
  årsak: ÅrsakType | "";
  beskrivelse: string;
  laster: boolean;
  feilmelding: string;
  erLagret: boolean;
  låst: boolean;
  oppdaterKravdato: (dato: Date | undefined) => void;
  oppdaterÅrsak: (årsak: ÅrsakType) => void;
  oppdaterBeskrivelse: (beskrivelse: string) => void;
  lagre: () => Promise<boolean>;
  settLåst: (val: boolean) => void;
  tilbakestill: () => void;
}

const tilLocalDateString = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
};

export const useArsakBehandling = (behandlingId: string): UseÅrsakBehandling => {
  const { årsakState, oppdaterÅrsakState, hentÅrsakData, årsakDataHentet, hentBehandlingPåNytt } =
    useBehandlingContext();

  const harEksisterendeData = årsakDataHentet && !!årsakState?.kravdato && !!årsakState?.årsak;

  const [laster, settLaster] = useState(false);
  const [erLagret, settErLagret] = useState(harEksisterendeData);
  const [låst, settLåst] = useState(harEksisterendeData);
  const [feilmelding, settFeilmelding] = useState("");

  const harSjekketInitiellLås = useRef(harEksisterendeData);

  if (årsakDataHentet && !harSjekketInitiellLås.current) {
    harSjekketInitiellLås.current = true;
    if (årsakState?.kravdato && årsakState?.årsak) {
      settLåst(true);
      settErLagret(true);
    }
  }

  useEffect(() => {
    if (!behandlingId || årsakDataHentet) return;

    const hentData = async () => {
      settLaster(true);
      settFeilmelding("");

      try {
        await hentÅrsakData();
      } catch {
        settFeilmelding("Kunne ikke hente eksisterende data");
      } finally {
        settLaster(false);
      }
    };

    hentData();
  }, [behandlingId, hentÅrsakData, årsakDataHentet]);

  const oppdaterKravdato = useCallback(
    (dato: Date | undefined) => {
      oppdaterÅrsakState({ kravdato: dato });
    },
    [oppdaterÅrsakState]
  );

  const oppdaterÅrsak = useCallback(
    (årsak: ÅrsakType) => {
      oppdaterÅrsakState({ årsak });
    },
    [oppdaterÅrsakState]
  );

  const oppdaterBeskrivelse = useCallback(
    (beskrivelse: string) => {
      oppdaterÅrsakState({ beskrivelse });
    },
    [oppdaterÅrsakState]
  );

  const tilbakestill = useCallback(() => {
    oppdaterÅrsakState({ kravdato: undefined, årsak: "" as ÅrsakType, beskrivelse: "" });
    settLåst(false);
    settErLagret(false);
  }, [oppdaterÅrsakState]);

  const lagre = useCallback(async (): Promise<boolean> => {
    const lagreÅrsakBehandling = (
      behandlingId: string,
      request: ÅrsakBehandlingRequest
    ): Promise<ApiResponse<ÅrsakBehandlingResponse>> => {
      return apiCall(`/arsak/${behandlingId}`, {
        method: "POST",
        body: JSON.stringify(request),
      });
    };

    if (!behandlingId || !årsakState?.kravdato || !årsakState?.årsak) {
      return false;
    }

    settLaster(true);
    settFeilmelding("");

    try {
      const response = await lagreÅrsakBehandling(behandlingId, {
        kravdato: tilLocalDateString(årsakState.kravdato),
        årsak: årsakState.årsak,
        beskrivelse: årsakState.beskrivelse,
      });

      if (response.data) {
        settErLagret(true);
        oppdaterEndringshistorikk();
        hentBehandlingPåNytt();
        return true;
      }

      settFeilmelding(response.melding || "Kunne ikke lagre årsak for behandling");
      return false;
    } catch {
      settFeilmelding("En uventet feil oppstod ved lagring");
      return false;
    } finally {
      settLaster(false);
    }
  }, [behandlingId, årsakState, hentBehandlingPåNytt]);

  return {
    kravdato: årsakState?.kravdato,
    årsak: årsakState?.årsak ?? "",
    beskrivelse: årsakState?.beskrivelse ?? "",
    laster,
    feilmelding,
    erLagret,
    låst,
    oppdaterKravdato,
    oppdaterÅrsak,
    oppdaterBeskrivelse,
    lagre,
    settLåst,
    tilbakestill,
  };
};
