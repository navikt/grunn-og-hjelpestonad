import { useCallback, useState } from "react";
import { fullførSteg } from "~/api/generated/sdk.gen";
import { feilmeldingFra } from "./feilmeldingFra";
import { useVilkårApiKall } from "./useVilkårApiKall";
import { valideringsfeilFra, type Valideringsfeil } from "./valideringsfeilFra";

type FullførStegOptions = Parameters<typeof fullførSteg>[0];

export interface FullførVilkårStegResult {
  fullfør: (onSuccess: () => void) => void;
  laster: boolean;
  valideringsfeil: Valideringsfeil[];
  feilmelding?: string;
}

export function useFullførVilkårSteg(behandlingId: string): FullførVilkårStegResult {
  const [valideringsfeil, settValideringsfeil] = useState<Valideringsfeil[]>([]);
  const [feilmelding, settFeilmelding] = useState<string | undefined>(undefined);

  const apiKall = useCallback(
    (options: FullførStegOptions) => fullførSteg({ ...options, throwOnError: true }),
    []
  );
  const { kall, laster } = useVilkårApiKall(apiKall);

  const fullfør = (onSuccess: () => void) => {
    settValideringsfeil([]);
    settFeilmelding(undefined);

    kall(
      { path: { behandlingId } },
      {
        onSuccess,
        onError: (error) => {
          const feil = valideringsfeilFra(error);
          if (feil.length > 0) {
            settValideringsfeil(feil);
          } else {
            settFeilmelding(feilmeldingFra(error, "Kunne ikke fullføre vilkårsvurderingen."));
          }
        },
      }
    );
  };

  return { fullfør, laster, valideringsfeil, feilmelding };
}
