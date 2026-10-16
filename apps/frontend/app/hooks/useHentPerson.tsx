import { useEffect, useState } from "react";
import { hentPerson } from "~/api/generated/sdk.gen";
import type { PersonResponse } from "~/api/generated/types.gen";
import type { Person } from "~/fellesContext/PersonContext";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

interface PersonState {
  person: Person | null;
  melding: string | null;
  laster: boolean;
}

export function useHentPdlNavn(fagsakPersonId: string | undefined) {
  const [state, settState] = useState<PersonState>({
    person: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentPersonFraPdl = async () => {
      if (!fagsakPersonId) {
        settState((prev) => ({
          ...prev,
          person: null,
          melding: null,
          laster: false,
        }));
        return;
      }

      settState((prev) => ({ ...prev, melding: null, laster: true }));

      try {
        const { data }: { data: PersonResponse } = await hentPerson({
          body: { fagsakPersonId },
          signal,
          throwOnError: true,
        });
        if (signal.aborted) return;
        settState((prev) => ({
          ...prev,
          person: {
            navn: {
              ...data.navn,
              mellomnavn: data.navn.mellomnavn ?? undefined,
            },
            fødselsdato: data.foedselsdato,
          },
          melding: null,
          laster: false,
        }));
      } catch (feil: unknown) {
        if (signal.aborted) return;
        settState((prev) => ({
          ...prev,
          person: null,
          melding: feilmeldingFra(feil, "Fant ikke navn i PDL"),
          laster: false,
        }));
      }
    };

    void hentPersonFraPdl();

    return () => controller.abort();
  }, [fagsakPersonId]);

  return state;
}
