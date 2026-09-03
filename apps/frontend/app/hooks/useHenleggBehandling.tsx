import { useState } from "react";
import { apiCall } from "~/api/backend";

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
      const response = await apiCall(`/behandling/henlegg`, {
        method: "POST",
        body: JSON.stringify({ behandlingId }),
      });

      if (response.melding) {
        settHenleggFeilmelding(response.melding);
        return false;
      }

      return true;
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
