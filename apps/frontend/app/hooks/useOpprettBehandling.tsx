import { useState } from "react";
import { opprettBehandling as opprettBehandlingApi } from "~/api/generated/sdk.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface OpprettBehandling {
  opprettBehandling: (fagsakId: string) => Promise<string | undefined>;
  oppretter: boolean;
  opprettFeilmelding: string | null;
}

export const useOpprettBehandling = (): OpprettBehandling => {
  const [oppretter, settOppretter] = useState(false);
  const [opprettFeilmelding, settOpprettFeilmelding] = useState<string | null>(null);

  const opprettBehandling = async (fagsakId: string): Promise<string | undefined> => {
    settOppretter(true);
    settOpprettFeilmelding(null);

    try {
      const { data } = await opprettBehandlingApi({
        body: { fagsakId },
        throwOnError: true,
      });
      return data.behandlingId;
    } catch (feil: unknown) {
      settOpprettFeilmelding(
        feilmeldingFra(feil, "Vi kunne ikke opprette behandlingen. Prøv igjen senere.")
      );
      return undefined;
    } finally {
      settOppretter(false);
    }
  };

  return {
    opprettBehandling,
    oppretter,
    opprettFeilmelding,
  };
};
