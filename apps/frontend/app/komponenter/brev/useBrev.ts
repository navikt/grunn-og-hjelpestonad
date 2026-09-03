import { useCallback, useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import { brevmaler } from "~/komponenter/brev/brevmaler";
import type { Brevmal, MellomlagretBrev, Tekstbolk } from "~/komponenter/brev/typer";

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
    brevmal: Brevmal,
    fritekstbolker: Tekstbolk[]
  ): Promise<ApiResponse<unknown>> => {
    settSender(true);
    try {
      return apiCall(`/brev/lag-task/${behandlingId}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          brevmal,
          fritekstbolker,
        }),
      });
    } finally {
      settSender(false);
    }
  };

  const mellomlagreBrev = useCallback(
    async (
      behandlingId: string,
      brevmal: Brevmal,
      fritekstbolker: Tekstbolk[]
    ): Promise<ApiResponse> => {
      try {
        return apiCall(`/brev/mellomlagre/${behandlingId}`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            brevmal,
            fritekstbolker,
          }),
        });
      } catch (error) {
        throw new Error("Feil ved mellomlagring av brev: " + error);
      }
    },
    []
  );

  const hentMellomlagretBrev = useCallback(() => {
    if (!behandlingId) return;

    return apiCall<MellomlagretBrev>(`/brev/hentMellomlagretBrev/${behandlingId}`, {
      method: "GET",
      headers: { Accept: "application/json" },
    }).then((res) => {
      if (!res.data) return;
      settBrevmal(res.data.brevmal ?? null);
      settFritekstbolker(res.data.fritekstbolker ?? []);
    });
  }, [behandlingId]);

  useEffect(() => {
    hentMellomlagretBrev();
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
