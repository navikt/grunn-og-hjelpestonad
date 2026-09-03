import { useState, useEffect, useCallback } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import type { Vurdering, VilkårType } from "~/types/vilkår";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";

export interface VilkårVurderingResponse {
  id: string | null;
  behandlingId: string;
  vilkårType: VilkårType;
  vurdering: Vurdering;
  begrunnelse: string;
  erVilkårOppfylt: boolean;
}

export interface VilkårVurderingRequest {
  vilkårType: VilkårType;
  vurdering: Vurdering;
  begrunnelse: string;
}

export interface VilkårState {
  spørsmålSvar: Vurdering | "";
  begrunnelse: string;
  låst: boolean;
  lagrer: boolean;
}

interface UseVilkårVurdering {
  vilkårState: Record<VilkårType, VilkårState>;
  laster: boolean;
  feilmelding: string;
  oppdaterVilkår: (vilkårType: VilkårType, data: Partial<VilkårState>) => void;
  lagreVilkår: (vilkårType: VilkårType) => Promise<boolean>;
  slettVilkår: (vilkårType: VilkårType) => void;
}

const initialVilkårState: VilkårState = {
  spørsmålSvar: "",
  begrunnelse: "",
  låst: false,
  lagrer: false,
};

const alleVilkårTyper: VilkårType[] = [
  "INNGANGSVILKÅR",
  "AKTIVITET",
  "INNTEKT",
  "ALDER_PÅ_BARN",
  "DOKUMENTASJON_TILSYNSUTGIFTER",
];

const lagInitialState = (): Record<VilkårType, VilkårState> => {
  return alleVilkårTyper.reduce(
    (acc, type) => {
      acc[type] = { ...initialVilkårState };
      return acc;
    },
    {} as Record<VilkårType, VilkårState>
  );
};

const mapVurderingTilSvar = (vurdering: Vurdering): Vurdering => vurdering;

export const useVilkårVurdering = (behandlingId: string): UseVilkårVurdering => {
  const [vilkårState, settVilkårState] = useState<Record<VilkårType, VilkårState>>(lagInitialState);
  const [laster, settLaster] = useState(false);
  const [feilmelding, settFeilmelding] = useState("");

  const hentVilkårVurderinger = async (
    behandlingId: string
  ): Promise<ApiResponse<VilkårVurderingResponse[]>> => {
    return apiCall(`/vilkar/${behandlingId}`);
  };

  const lagreVilkårVurdering = async (
    behandlingId: string,
    request: VilkårVurderingRequest
  ): Promise<ApiResponse<VilkårVurderingResponse>> => {
    return apiCall(`/vilkar/${behandlingId}`, {
      method: "POST",
      body: JSON.stringify(request),
    });
  };

  useEffect(() => {
    if (!behandlingId) return;

    const hentData = async () => {
      settLaster(true);
      settFeilmelding("");

      try {
        const response = await hentVilkårVurderinger(behandlingId);
        if (response.data) {
          const nyState = lagInitialState();

          response.data.forEach((vurdering: VilkårVurderingResponse) => {
            nyState[vurdering.vilkårType] = {
              spørsmålSvar: mapVurderingTilSvar(vurdering.vurdering),
              begrunnelse: vurdering.begrunnelse,
              låst: true,
              lagrer: false,
            };
          });

          settVilkårState(nyState);
        }
      } catch {
        settFeilmelding("Kunne ikke hente vilkårvurderinger");
      } finally {
        settLaster(false);
      }
    };

    hentData();
  }, [behandlingId]);

  const oppdaterVilkår = useCallback((vilkårType: VilkårType, data: Partial<VilkårState>) => {
    settVilkårState((prev) => ({
      ...prev,
      [vilkårType]: { ...prev[vilkårType], ...data },
    }));
  }, []);

  const lagreVilkår = useCallback(
    async (vilkårType: VilkårType): Promise<boolean> => {
      const vilkår = vilkårState[vilkårType];
      if (!behandlingId || !vilkår.spørsmålSvar) {
        return false;
      }

      oppdaterVilkår(vilkårType, { lagrer: true });
      settFeilmelding("");

      try {
        const response = await lagreVilkårVurdering(behandlingId, {
          vilkårType,
          vurdering: vilkår.spørsmålSvar as Vurdering,
          begrunnelse: vilkår.begrunnelse,
        });

        if (response.data) {
          oppdaterVilkår(vilkårType, { låst: true, lagrer: false });
          oppdaterEndringshistorikk();
          return true;
        }

        settFeilmelding(response.melding || "Kunne ikke lagre vilkårvurdering");
        oppdaterVilkår(vilkårType, { lagrer: false });
        return false;
      } catch {
        settFeilmelding("En uventet feil oppstod ved lagring");
        oppdaterVilkår(vilkårType, { lagrer: false });
        return false;
      }
    },
    [behandlingId, vilkårState, oppdaterVilkår]
  );

  const slettVilkår = useCallback(
    (vilkårType: VilkårType) => {
      oppdaterVilkår(vilkårType, {
        spørsmålSvar: "",
        begrunnelse: "",
        låst: false,
      });
    },
    [oppdaterVilkår]
  );

  return {
    vilkårState,
    laster,
    feilmelding,
    oppdaterVilkår,
    lagreVilkår,
    slettVilkår,
  };
};
