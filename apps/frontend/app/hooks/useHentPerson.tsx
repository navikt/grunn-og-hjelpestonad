import { useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import type { Navn, Person } from "~/contexts/PersonContext";

interface PersonState {
  person: Person | null;
  melding: string | null;
  laster: boolean;
}

interface PdlPersonResponse {
  navn: Navn;
  foedselsdato: string;
}

export function useHentPdlNavn(fagsakPersonId: string | undefined) {
  const [state, settState] = useState<PersonState>({
    person: null,
    melding: null,
    laster: true,
  });

  useEffect(() => {
    const hentPersonFraPdl = async (
      fagsakPersonId: string
    ): Promise<ApiResponse<PdlPersonResponse>> => {
      return apiCall(`/pdl/person`, {
        method: "POST",
        body: JSON.stringify({ fagsakPersonId }),
      });
    };

    let avbrutt = false;

    const hentPerson = async () => {
      if (avbrutt) return;

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

      const response = await hentPersonFraPdl(fagsakPersonId);

      if (avbrutt) return;

      if (response.data) {
        const pdlPerson = response.data;

        settState((prev) => ({
          ...prev,
          person: {
            navn: pdlPerson.navn,
            fødselsdato: pdlPerson.foedselsdato,
          },
          laster: false,
        }));
      } else {
        settState((prev) => ({
          ...prev,
          person: null,
          melding: response.melding ?? "Fant ikke navn i PDL",
          laster: false,
        }));
      }
    };

    hentPerson();

    return () => {
      avbrutt = true;
    };
  }, [fagsakPersonId]);

  return state;
}
