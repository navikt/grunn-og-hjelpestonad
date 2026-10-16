import { useState } from "react";
import { fordelOppgave } from "~/api/generated/sdk.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

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

    try {
      await fordelOppgave({
        body: { behandlingId, saksbehandler },
        throwOnError: true,
      });
    } catch (feil: unknown) {
      const erIkkeFunnet =
        feil !== null &&
        typeof feil === "object" &&
        "status" in feil &&
        (feil.status === "404" || feil.status === 404);
      settState({
        laster: false,
        feilmelding: erIkkeFunnet
          ? "Ingen oppgave funnet for denne behandlingen. Oppgaven kan ikke tildeles."
          : feilmeldingFra(feil, "Kunne ikke tildele oppgaven. Prøv igjen senere."),
      });
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
