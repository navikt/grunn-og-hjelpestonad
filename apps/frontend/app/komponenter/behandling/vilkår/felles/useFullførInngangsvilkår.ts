import { useCallback, useRef, useState } from "react";
import { fullførInngangsvilkår } from "~/api/generated/sdk.gen";
import type {
  InngangsvilkårBlokkertResponse,
  InngangsvilkårResponse,
} from "~/api/generated/types.gen";
import { feilmeldingFra } from "./feilmeldingFra";
import { inngangsvilkårBlokkertFeilFra } from "./inngangsvilkårBlokkertFeilFra";
import { useApiKall } from "./useApiKall";
import { valideringsfeilFra, type Valideringsfeil } from "./valideringsfeilFra";

type FullførOptions = Parameters<typeof fullførInngangsvilkår>[0];

export interface FullførInngangsvilkårResult {
  fullfør: (onSuccess: (resultat: InngangsvilkårResponse) => void) => void;
  laster: boolean;
  valideringsfeil: Valideringsfeil[];
  feilmelding?: string;
  blokkertFeil?: InngangsvilkårBlokkertResponse;
}

export function useFullførInngangsvilkår(behandlingId: string): FullførInngangsvilkårResult {
  const [valideringsfeil, settValideringsfeil] = useState<Valideringsfeil[]>([]);
  const [feilmelding, settFeilmelding] = useState<string | undefined>(undefined);
  const [blokkertFeil, settBlokkertFeil] = useState<InngangsvilkårBlokkertResponse | undefined>(
    undefined
  );
  const resultatRef = useRef<InngangsvilkårResponse | undefined>(undefined);

  const apiKall = useCallback(async (options: FullførOptions) => {
    const { data } = await fullførInngangsvilkår({ ...options, throwOnError: true });
    resultatRef.current = data;
    return data;
  }, []);
  const { kall, laster } = useApiKall(apiKall);

  const fullfør = (onSuccess: (resultat: InngangsvilkårResponse) => void) => {
    settValideringsfeil([]);
    settFeilmelding(undefined);
    settBlokkertFeil(undefined);

    kall(
      { path: { behandlingId } },
      {
        onSuccess: () => {
          settBlokkertFeil(undefined);
          const resultat = resultatRef.current;
          if (resultat) onSuccess(resultat);
        },
        onError: (error) => {
          const blokkertFeilFraBackend = inngangsvilkårBlokkertFeilFra(error);
          if (blokkertFeilFraBackend) {
            settValideringsfeil([]);
            settFeilmelding(undefined);
            settBlokkertFeil(blokkertFeilFraBackend);
            return;
          }

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

  return { fullfør, laster, valideringsfeil, feilmelding, blokkertFeil };
}
