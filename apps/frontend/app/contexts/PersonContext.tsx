import { createContext, useContext } from "react";
import type { FagsakDto } from "~/hooks/useFagsak";

export interface Person {
  navn: Navn;
  fødselsdato: string;
}

export interface Navn {
  fornavn: string;
  mellomnavn?: string;
  etternavn: string;
}

interface PersonContextType {
  person: Person | null;
  personident: string;
  fagsakPersonId: string;
  fagsak: FagsakDto | null;
  fagsakId: string | undefined;
  laster: boolean;
}

// TODO: Rename etter at fagsak også er lagt til?
const PersonContext = createContext<PersonContextType | null>(null);

export function usePersonContext() {
  const context = useContext(PersonContext);
  if (!context) {
    throw new Error("usePersonContext må brukes innenfor PersonProvider");
  }
  return context;
}

export { PersonContext };
export type { PersonContextType };
