import { useState } from "react";
import { henleggBehandling as henleggBehandlingApi } from "~/api/generated/sdk.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface HenleggBehandling {
  henleggBehandling: (behandlingId: string) => Promise<boolean>;
  laster: boolean;
  henleggFeilmelding: string | null;
}

export const useHenleggBehandling = (): HenleggBehandling => {
  const [laster, settLaster] = useState(false);
  const [henleggFeilmelding, settHenleggFeilmelding] = useState<string | null>(null);

  const henleggBehandling = async (behandlingId: string): Promise<boolean> => {
    settLaster(true);
    settHenleggFeilmelding(null);

    try {
      await henleggBehandlingApi({
        body: { behandlingId },
        throwOnError: true,
      });

      return true;
    } catch (feil: unknown) {
      settHenleggFeilmelding(feilmeldingFra(feil, "Kunne ikke henlegge behandling"));
      return false;
    } finally {
      settLaster(false);
    }
  };

  return {
    henleggBehandling,
    laster: laster,
    henleggFeilmelding,
  };
};
