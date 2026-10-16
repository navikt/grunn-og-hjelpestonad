import { useCallback, useEffect, useState } from "react";
import {
  hentMellomlagretBrev as hentMellomlagretBrevKall,
  lagBrevTask,
  mellomlagreBrev as mellomlagreBrevKall,
} from "~/api/generated/sdk.gen";
import type { BrevResponse } from "~/api/generated/types.gen";
import { brevmaler } from "~/komponenter/brev/brevmaler";
import type { Brevmal, Tekstbolk } from "~/komponenter/brev/typer";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

export const useBrev = (behandlingId?: string) => {
  const [brevMal, settBrevmal] = useState<Brevmal | null>(null);
  const [fritekstbolker, settFritekstbolker] = useState<Tekstbolk[]>([]);
  const [sender, settSender] = useState(false);

  const leggTilFritekstbolk = () => {
    settFritekstbolker((prev) => [...prev, { underoverskrift: "", innhold: "" }]);
  };

  const slettFritekstbolk = (index: number) => {
    settFritekstbolker((prev) => prev.filter((_, i) => i !== index));
  };

  const flyttBolkOpp = (index: number) => {
    settFritekstbolker((prev) => {
      if (index === 0) return prev;
      const newArr = [...prev];
      [newArr[index - 1], newArr[index]] = [newArr[index], newArr[index - 1]];
      return newArr;
    });
  };

  const flyttBolkNed = (index: number) => {
    settFritekstbolker((prev) => {
      if (index === prev.length - 1) return prev;
      const newArr = [...prev];
      [newArr[index + 1], newArr[index]] = [newArr[index], newArr[index + 1]];
      return newArr;
    });
  };

  const oppdaterFelt = (index: number, partial: Partial<Tekstbolk>) => {
    settFritekstbolker((prev) => {
      const newArr = [...prev];
      newArr[index] = { ...newArr[index], ...partial };
      return newArr;
    });
  };

  const velgBrevmal = (brevmal: string): void => {
    if (brevmal === "") {
      settBrevmal(null);
    } else {
      const valgtBrevmal = brevmaler.find((b) => b.tittel === brevmal) ?? null;
      settBrevmal(valgtBrevmal);
    }
  };

  const sendPdfTilSak = async (
    behandlingId: string,
    _brevmal: Brevmal,
    _fritekstbolker: Tekstbolk[]
  ): Promise<{ data?: unknown; feilmelding?: string }> => {
    settSender(true);
    try {
      const { data } = await lagBrevTask({
        path: { behandlingId },
        throwOnError: true,
      });
      return { data };
    } catch (feil: unknown) {
      return { feilmelding: feilmeldingFra(feil, "Kunne ikke sende brev til sak.") };
    } finally {
      settSender(false);
    }
  };

  const mellomlagreBrev = useCallback(
    async (
      behandlingId: string,
      brevmal: Brevmal,
      fritekstbolker: Tekstbolk[]
    ): Promise<{ data?: unknown }> => {
      try {
        const { data } = await mellomlagreBrevKall({
          path: { behandlingId },
          body: {
            brevmal,
            fritekstbolker,
          },
          throwOnError: true,
        });
        return { data };
      } catch (feil: unknown) {
        throw new Error(
          "Feil ved mellomlagring av brev: " +
            feilmeldingFra(feil, "Feil ved mellomlagring av brev.")
        );
      }
    },
    []
  );

  const hentMellomlagretBrev = useCallback(
    async (signal: AbortSignal) => {
      if (!behandlingId) return;

      try {
        const { data, response }: { data: BrevResponse; response: Response } =
          await hentMellomlagretBrevKall({
            path: { behandlingId },
            signal,
            throwOnError: true,
          });
        if (signal.aborted) return;
        if (response.status === 204) return;
        settBrevmal({
          ...data.brevmal,
          fastTekstAvslutning: data.brevmal.fastTekstAvslutning.map(
            ({ underoverskrift, ...bolk }) => ({
              ...bolk,
              underoverskrift: underoverskrift ?? undefined,
            })
          ),
        });
        settFritekstbolker(
          data.fritekstbolker.map(({ underoverskrift, ...bolk }) => ({
            ...bolk,
            underoverskrift: underoverskrift ?? undefined,
          }))
        );
      } catch {
        if (signal.aborted) return;
        console.error("Kunne ikke hente mellomlagret brev.");
      }
    },
    [behandlingId]
  );

  useEffect(() => {
    const controller = new AbortController();
    const timer = setTimeout(() => void hentMellomlagretBrev(controller.signal), 0);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [hentMellomlagretBrev]);

  return {
    brevMal,
    fritekstbolker,
    sender,
    leggTilFritekstbolk,
    flyttBolkOpp,
    flyttBolkNed,
    oppdaterFelt,
    velgBrevmal,
    sendPdfTilSak,
    mellomlagreBrev,
    slettFritekstbolk,
  };
};
