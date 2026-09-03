import { createContext, useContext } from "react";

interface LesevisningContextType {
  erLesevisning: boolean;
  settErLesevisning: (verdi: boolean) => void;
}

const LesevisningsContext = createContext<LesevisningContextType | null>(null);

export function useLesevisningsContext() {
  const context = useContext(LesevisningsContext);

  if (!context) {
    throw new Error("useLesevisningsContext må brukes innenfor LesevisningsProvider");
  }

  return context;
}

export { LesevisningsContext };
export type { LesevisningContextType };
