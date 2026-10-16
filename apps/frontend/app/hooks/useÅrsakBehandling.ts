import { useState, useEffect, useCallback } from "react";
import type { ÅrsakType } from "~/types/årsak";
import type { ÅrsakBehandlingResponse } from "~/api/generated/types.gen";
import { lagreÅrsakBehandling } from "~/api/generated/sdk.gen";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

export type { ÅrsakBehandlingResponse };

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
  const [erLagretOverstyring, settErLagretOverstyring] = useState<boolean | null>(null);
  const [låstOverstyring, settLåstOverstyring] = useState<boolean | null>(null);
  const [feilmelding, settFeilmelding] = useState("");

  const erLagret = erLagretOverstyring ?? harEksisterendeData;
  const låst = låstOverstyring ?? harEksisterendeData;

  const settLåst = useCallback((val: boolean) => settLåstOverstyring(val), []);

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
    settLåstOverstyring(false);
    settErLagretOverstyring(false);
  }, [oppdaterÅrsakState]);

  const lagre = useCallback(async (): Promise<boolean> => {
    if (!behandlingId || !årsakState?.kravdato || !årsakState?.årsak) {
      return false;
    }

    settLaster(true);
    settFeilmelding("");

    try {
      await lagreÅrsakBehandling({
        path: { behandlingId },
        body: {
          kravdato: tilLocalDateString(årsakState.kravdato),
          årsak: årsakState.årsak,
          beskrivelse: årsakState.beskrivelse,
        },
        throwOnError: true,
      });

      settErLagretOverstyring(true);
      oppdaterEndringshistorikk();
      hentBehandlingPåNytt();
      return true;
    } catch (feil: unknown) {
      settFeilmelding(feilmeldingFra(feil, "En uventet feil oppstod ved lagring"));
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
