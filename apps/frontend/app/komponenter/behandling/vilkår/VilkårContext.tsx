import React, { createContext, useContext } from "react";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { lagVilkårStatus, type VilkårStatus } from "./vilkårStatus";
import { hentDiagnosePerioderFraBackend } from "./diagnose/useDiagnoseVilkår";
import { hentMedlemskapPerioderFraBackend } from "./medlemskap/useMedlemskapVilkår";
import { usePerioder, type PerioderResult } from "./felles/usePerioder";
import { usePdlFaktagrunnlag } from "../faktagrunnlag/usePdlFaktagrunnlag";
import type { VilkårDiagnoseResponse, VilkårMedlemskapResponse } from "~/api/generated/types.gen";

type PdlFaktagrunnlag = ReturnType<typeof usePdlFaktagrunnlag>;

interface VilkårContextValue {
  medlemskapsPerioder: PerioderResult<VilkårMedlemskapResponse>;
  diagnosePerioder: PerioderResult<VilkårDiagnoseResponse>;
  medlemskapFaktagrunnlagTilstand: PdlFaktagrunnlag["medlemskapFaktagrunnlagTilstand"];
  falskIdentitetTilstand: PdlFaktagrunnlag["falskIdentitetTilstand"];
  status: VilkårStatus;
}

const VilkårContext = createContext<VilkårContextValue | undefined>(undefined);

export function VilkårProvider({ children }: { children: React.ReactNode }) {
  const { behandlingId } = useBehandlingContext();
  const medlemskapsPerioder = usePerioder(behandlingId, hentMedlemskapPerioderFraBackend);
  const diagnosePerioder = usePerioder(behandlingId, hentDiagnosePerioderFraBackend);
  const { medlemskapFaktagrunnlagTilstand, falskIdentitetTilstand } =
    usePdlFaktagrunnlag(behandlingId);

  const status = lagVilkårStatus({
    medlemskapsPerioder,
    diagnosePerioder,
  });

  return (
    <VilkårContext.Provider
      value={{
        diagnosePerioder,
        medlemskapsPerioder,
        medlemskapFaktagrunnlagTilstand,
        falskIdentitetTilstand,
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
