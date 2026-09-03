import { useState } from "react";
import { apiCall } from "~/api/backend";

interface TildelOppgaveState {
  laster: boolean;
  feilmelding: string | null;
}

export const useTildelOppgave = () => {
  const [state, settState] = useState<TildelOppgaveState>({
    laster: false,
    feilmelding: null,
  });

  const tildelOppgave = async (behandlingId: string, saksbehandler: string): Promise<boolean> => {
    settState({ laster: true, feilmelding: null });

    const response = await apiCall<number>("/oppgave/fordel", {
      method: "POST",
      body: JSON.stringify({ behandlingId, saksbehandler }),
    });

    if (response.status === "404") {
      settState({
        laster: false,
        feilmelding: "Ingen oppgave funnet for denne behandlingen. Oppgaven kan ikke tildeles.",
      });
      return false;
    }

    if (response.melding) {
      settState({ laster: false, feilmelding: response.melding });
      return false;
    }

    settState({ laster: false, feilmelding: null });
    return true;
  };

  return {
    tildelOppgave,
    laster: state.laster,
    feilmelding: state.feilmelding,
  };
};
