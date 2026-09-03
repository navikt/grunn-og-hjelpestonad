import { useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import { usePersonContext } from "~/contexts/PersonContext";

export enum BrevmottakerRolle {
  BRUKER = "BRUKER",
  VERGE = "VERGE",
  FULLMEKTIG = "FULLMEKTIG",
  ANNEN = "ANNEN",
}

export enum MottakerType {
  PERSON = "PERSON",
  ORGANISASJON = "ORGANISASJON",
}

export interface Brevmottaker {
  personRolle?: BrevmottakerRolle;
  mottakerType: MottakerType;
  personident?: string;
  orgnr?: string;
  navnHosOrganisasjon?: string;
}

export const useBrevmottaker = (behandlingId?: string) => {
  const { personident } = usePersonContext();
  const [mottakere, settMottakere] = useState<Brevmottaker[]>([]);

  useEffect(() => {
    if (!behandlingId) return;

    const hentBrevmottakere = async () => {
      const response = await apiCall<Brevmottaker[]>(`/brevmottaker/${behandlingId}`);

      if (response.data && response.data.length > 0) {
        settMottakere(response.data);
      } else {
        const søker: Brevmottaker = {
          mottakerType: MottakerType.PERSON,
          personRolle: BrevmottakerRolle.BRUKER,
          personident: personident,
        };
        settMottakere([søker]);
        await sendMottakereTilSak(behandlingId, [søker]);
      }
    };

    hentBrevmottakere();
  }, [behandlingId, personident]);

  const leggTilMottaker = (mottaker: Brevmottaker) => {
    settMottakere((prev) => [...prev, mottaker]);
  };

  const fjernMottaker = (index: number) => {
    settMottakere((prev) => prev.filter((_, i) => i !== index));
  };

  const utledBrevmottakere = () => {
    return mottakere
      .map((mottaker) => {
        if (mottaker.mottakerType === MottakerType.ORGANISASJON) {
          return `${mottaker.orgnr} v/ ${mottaker.navnHosOrganisasjon} (organisasjon)`;
        }
        switch (mottaker.personRolle) {
          case BrevmottakerRolle.BRUKER:
            return `${mottaker.personident} (Bruker)`;
          case BrevmottakerRolle.VERGE:
            return `${mottaker.personident} (Verge)`;
          case BrevmottakerRolle.FULLMEKTIG:
            return `${mottaker.personident} (Fullmektig)`;
          default:
            return "";
        }
      })
      .join(", ");
  };

  const sendMottakereTilSak = async (
    behandlingId: string,
    brevmottakere: Brevmottaker[]
  ): Promise<ApiResponse<unknown>> => {
    return apiCall(`/brevmottaker/settMottakere/${behandlingId}`, {
      method: "POST",
      body: JSON.stringify(brevmottakere),
    });
  };

  return {
    mottakere,
    settMottakere,
    leggTilMottaker,
    fjernMottaker,
    utledBrevmottakere,
    sendMottakereTilSak,
  };
};
