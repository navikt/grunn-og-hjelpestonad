import { useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import type { Dokumentinfo } from "~/api/dokument";

interface DokumenterState {
  dokumenter: [Dokumentinfo] | null;
  melding: string | null;
  laster: boolean;
}

export function useHentDokumenter(fagsakPersonId: string | undefined) {
  const [state, settState] = useState<DokumenterState>({
    dokumenter: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const hentDokumenterForPerson = async (
      fagsakPersonId: string
    ): Promise<ApiResponse<[Dokumentinfo]>> => {
      return apiCall(`/saf/dokumenter`, {
        method: "POST",
        body: JSON.stringify({ fagsakPersonId }),
      });
    };

    let avbrutt = false;

    const hentDokumenter = async () => {
      if (avbrutt) return;

      if (!fagsakPersonId) {
        settState((prev) => ({
          ...prev,
          melding: "Mangler fagsakId",
          laster: false,
        }));
        return;
      }

      settState((prev) => ({ ...prev, melding: null, laster: true }));

      const response = await hentDokumenterForPerson(fagsakPersonId);

      if (avbrutt) return;

      if (response.data) {
        settState((prev) => ({
          ...prev,
          dokumenter: response.data ?? null,
          laster: false,
        }));
      }
    };

    hentDokumenter();

    return () => {
      avbrutt = true;
    };
  }, [fagsakPersonId]);

  return state;
}
