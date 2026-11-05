import { useCallback, useRef, useState } from "react";
import { fullførInngangsvilkår } from "~/api/generated/sdk.gen";
import type { InngangsvilkårResponse } from "~/api/generated/types.gen";
import { feilmeldingFra } from "./feilmeldingFra";
import { useVilkårApiKall } from "./useVilkårApiKall";
import { valideringsfeilFra, type Valideringsfeil } from "./valideringsfeilFra";

type FullførOptions = Parameters<typeof fullførInngangsvilkår>[0];

export interface FullførInngangsvilkårResult {
  fullfør: (onSuccess: (resultat: InngangsvilkårResponse) => void) => void;
  laster: boolean;
  valideringsfeil: Valideringsfeil[];
  feilmelding?: string;
}

export function useFullførInngangsvilkår(behandlingId: string): FullførInngangsvilkårResult {
  const [valideringsfeil, settValideringsfeil] = useState<Valideringsfeil[]>([]);
  const [feilmelding, settFeilmelding] = useState<string | undefined>(undefined);
  const resultatRef = useRef<InngangsvilkårResponse | undefined>(undefined);

  const apiKall = useCallback(async (options: FullførOptions) => {
    const { data } = await fullførInngangsvilkår({ ...options, throwOnError: true });
    resultatRef.current = data;
    return data;
  }, []);
  const { kall, laster } = useVilkårApiKall(apiKall);

  const fullfør = (onSuccess: (resultat: InngangsvilkårResponse) => void) => {
    settValideringsfeil([]);
    settFeilmelding(undefined);

    kall(
      { path: { behandlingId } },
      {
        onSuccess: () => {
          const resultat = resultatRef.current;
          if (resultat) onSuccess(resultat);
        },
        onError: (error) => {
          const feil = valideringsfeilFra(error);
          if (feil.length > 0) {
            settValideringsfeil(feil);
          } else {
            settFeilmelding(
              feilmeldingFra(error, "Kunne ikke fullføre vurderingen av inngangsvilkår.")
            );
          }
        },
      }
    );
  };

  return { fullfør, laster, valideringsfeil, feilmelding };
}
