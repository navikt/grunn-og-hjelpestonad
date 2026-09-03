import { useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";

interface HistorikkState {
  data: unknown | null;
  melding: string | null;
  laster: boolean;
}

export const useHentInfotrygdHistorikk = (personident: string | undefined) => {
  const [state, settState] = useState<HistorikkState>({
    data: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const hentHistorikkForPerson = async (personident: string): Promise<ApiResponse<unknown>> => {
      return apiCall(`/test/infotrygd/perioder`, {
        method: "POST",
        body: JSON.stringify({ personident: personident }),
      });
    };

    let avbrutt = false;

    const hentHistorikk = async () => {
      if (!personident) {
        settState({
          data: null,
          melding: null,
          laster: false,
        });
        return;
      }

      const response = await hentHistorikkForPerson(personident);

      if (avbrutt) return;

      if (response.data) {
        settState({
          data: response.data,
          melding: null,
          laster: false,
        });
      } else {
        settState({
          data: null,
          melding: response.melding || null,
          laster: false,
        });
      }
    };

    hentHistorikk();

    return () => {
      avbrutt = true;
    };
  }, [personident]);

  return state;
};
