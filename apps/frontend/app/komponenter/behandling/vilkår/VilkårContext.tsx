import React, { createContext, useContext } from "react";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { lagVilkårStatus, type VilkårStatus } from "./vilkårStatus";
import { hentDiagnosePerioderFraBackend } from "./diagnose/useDiagnoseVilkår";
import { hentMedlemskapPerioderFraBackend } from "./medlemskap/useMedlemskapVilkår";
import { usePerioder, type PerioderResult } from "./felles/usePerioder";
import type { VilkårDiagnoseResponse, VilkårMedlemskapResponse } from "~/api/generated/types.gen";

interface VilkårContextValue {
  medlemskapsPerioder: PerioderResult<VilkårMedlemskapResponse>;
  diagnosePerioder: PerioderResult<VilkårDiagnoseResponse>;
  status: VilkårStatus;
}

const VilkårContext = createContext<VilkårContextValue | undefined>(undefined);

export function VilkårProvider({ children }: { children: React.ReactNode }) {
  const { behandlingId } = useBehandlingContext();
  const medlemskapsPerioder = usePerioder(behandlingId, hentMedlemskapPerioderFraBackend);
  const diagnosePerioder = usePerioder(behandlingId, hentDiagnosePerioderFraBackend);

  const status = lagVilkårStatus({
    medlemskapsPerioder,
    diagnosePerioder,
  });

  return (
    <VilkårContext.Provider
      value={{
        diagnosePerioder,
        medlemskapsPerioder,
        status,
      }}
    >
      {children}
    </VilkårContext.Provider>
  );
}

export function useVilkårContext() {
  const context = useContext(VilkårContext);
  if (context === undefined) {
    throw new Error("useVilkårContext må brukes innenfor VilkårProvider");
  }
  return context;
}
