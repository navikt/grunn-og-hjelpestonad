import { useEffect, useState } from "react";
import {
  hentBrevmottakere as hentBrevmottakereKall,
  oppdaterBrevmottakere,
} from "~/api/generated/sdk.gen";
import type { BrevmottakerRequest, BrevmottakerResponse } from "~/api/generated/types.gen";
import { usePersonContext } from "~/fellesContext/PersonContext";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

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

export type Brevmottaker = Omit<BrevmottakerResponse, "personRolle"> & {
  personRolle?: BrevmottakerRolle | BrevmottakerResponse["personRolle"];
};

export const useBrevmottaker = (behandlingId?: string) => {
  const { personident } = usePersonContext();
  const [mottakere, settMottakere] = useState<Brevmottaker[]>([]);

  const sendMottakereTilSak = async (
    behandlingId: string,
    brevmottakere: Brevmottaker[]
  ): Promise<{ data?: unknown; feilmelding?: string }> => {
    try {
      const { data } = await oppdaterBrevmottakere({
        path: { behandlingId },
        body: brevmottakere as BrevmottakerRequest[],
        throwOnError: true,
      });
      return { data };
    } catch (feil: unknown) {
      return { feilmelding: feilmeldingFra(feil, "Kunne ikke lagre brevmottakere.") };
    }
  };

  useEffect(() => {
    if (!behandlingId) return;

    const controller = new AbortController();
    const { signal } = controller;

    const hentBrevmottakere = async () => {
      try {
        const { data }: { data: BrevmottakerResponse[] } = await hentBrevmottakereKall({
          path: { behandlingId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;

        if (data.length > 0) {
          settMottakere(data);
        } else {
          const søker: Brevmottaker = {
            mottakerType: MottakerType.PERSON,
            personRolle: BrevmottakerRolle.BRUKER,
            personident: personident,
          };
          settMottakere([søker]);
          await sendMottakereTilSak(behandlingId, [søker]);
        }
      } catch {
        if (signal.aborted) return;
        return;
      }
    };

    void hentBrevmottakere();
    return () => controller.abort();
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

  return {
    mottakere,
    settMottakere,
    leggTilMottaker,
    fjernMottaker,
    utledBrevmottakere,
    sendMottakereTilSak,
  };
};
