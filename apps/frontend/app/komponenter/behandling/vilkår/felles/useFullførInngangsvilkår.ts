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

export type FullførInngangsvilkårFeil =
  | { type: "validering"; feil: Valideringsfeil[] }
  | { type: "blokkert"; feil: InngangsvilkårBlokkertResponse }
  | { type: "generell"; melding: string };

export interface FullførInngangsvilkårResult {
  fullfør: (onSuccess: (resultat: InngangsvilkårResponse) => void) => void;
  laster: boolean;
  feil?: FullførInngangsvilkårFeil;
}

export function useFullførInngangsvilkår(behandlingId: string): FullførInngangsvilkårResult {
  const [feil, settFeil] = useState<FullførInngangsvilkårFeil | undefined>(undefined);
  const resultatRef = useRef<InngangsvilkårResponse | undefined>(undefined);

  const apiKall = useCallback(async (options: FullførOptions) => {
    const { data } = await fullførInngangsvilkår({ ...options, throwOnError: true });
    resultatRef.current = data;
    return data;
  }, []);
  const { kall, laster } = useApiKall(apiKall);

  const fullfør = (onSuccess: (resultat: InngangsvilkårResponse) => void) => {
    settFeil(undefined);

    kall(
      { path: { behandlingId } },
      {
        onSuccess: () => {
          const resultat = resultatRef.current;
          if (resultat) onSuccess(resultat);
        },
        onError: (error) => {
          const blokkertFeilFraBackend = inngangsvilkårBlokkertFeilFra(error);
          if (blokkertFeilFraBackend) {
            settFeil({ type: "blokkert", feil: blokkertFeilFraBackend });
            return;
          }

          const valideringsfeilFraBackend = valideringsfeilFra(error);
          if (valideringsfeilFraBackend.length > 0) {
            settFeil({ type: "validering", feil: valideringsfeilFraBackend });
          } else {
            settFeil({
              type: "generell",
              melding: feilmeldingFra(error, "Kunne ikke fullføre vurderingen av inngangsvilkår."),
            });
          }
        },
      }
    );
  };

  return { fullfør, laster, feil };
}
