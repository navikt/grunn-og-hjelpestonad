import React, { useEffect, useMemo } from "react";
import { Alert, VStack } from "@navikt/ds-react";
import Inngangsvilkår from "./vilkår/Inngangsvilkår";
import Aktivitet from "./vilkår/Aktivitet";
import Inntekt from "./vilkår/Inntekt";
import AlderPåBarn from "./vilkår/AlderPåBarn";
import DokumentasjonTilsynsutgifter from "./vilkår/DokumentasjonTilsynsutgifter";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import { useVilkårVurdering, type VilkårState } from "~/hooks/useVilkårVurdering";
import type { VilkårType, Vurdering } from "~/types/vilkår";

export const VilkårInnhold: React.FC<{
  settErVilkårUtfylt: React.Dispatch<React.SetStateAction<boolean>>;
}> = ({ settErVilkårUtfylt }) => {
  const { behandlingId } = useBehandlingContext();
  const { vilkårState, laster, feilmelding, oppdaterVilkår, lagreVilkår, slettVilkår } =
    useVilkårVurdering(behandlingId);

  const alleVilkårHarSvar = useMemo(() => {
    const erVilkårFerdigUtfylt = (vilkår: VilkårState) => {
      return vilkår.spørsmålSvar !== "" && vilkår.begrunnelse.trim() !== "" && vilkår.låst;
    };

    return (
      erVilkårFerdigUtfylt(vilkårState["INNGANGSVILKÅR"]) &&
      erVilkårFerdigUtfylt(vilkårState["AKTIVITET"]) &&
      erVilkårFerdigUtfylt(vilkårState["INNTEKT"]) &&
      erVilkårFerdigUtfylt(vilkårState["ALDER_PÅ_BARN"]) &&
      erVilkårFerdigUtfylt(vilkårState["DOKUMENTASJON_TILSYNSUTGIFTER"])
    );
  }, [vilkårState]);

  useEffect(() => {
    settErVilkårUtfylt(alleVilkårHarSvar);
  }, [alleVilkårHarSvar, settErVilkårUtfylt]);

  const lagVilkårProps = (vilkårType: VilkårType) => ({
    spørsmålSvar: vilkårState[vilkårType].spørsmålSvar,
    settSpørsmålSvar: (val: string) =>
      oppdaterVilkår(vilkårType, { spørsmålSvar: val as Vurdering | "" }),
    begrunnelse: vilkårState[vilkårType].begrunnelse,
    settBegrunnelse: (val: string) => oppdaterVilkår(vilkårType, { begrunnelse: val }),
    låst: vilkårState[vilkårType].låst,
    settLåst: (val: boolean) => oppdaterVilkår(vilkårType, { låst: val }),
    lagrer: vilkårState[vilkårType].lagrer,
    onLagre: () => lagreVilkår(vilkårType),
    onSlett: () => slettVilkår(vilkårType),
  });

  if (laster) {
    return <div>Laster vilkårvurderinger...</div>;
  }

  return (
    <VStack gap="space-24">
      {feilmelding && (
        <Alert variant="error" size="small">
          {feilmelding}
        </Alert>
      )}

      <Inngangsvilkår {...lagVilkårProps("INNGANGSVILKÅR")} />
      <Aktivitet {...lagVilkårProps("AKTIVITET")} />
      <Inntekt {...lagVilkårProps("INNTEKT")} />
      <AlderPåBarn {...lagVilkårProps("ALDER_PÅ_BARN")} />
      <DokumentasjonTilsynsutgifter {...lagVilkårProps("DOKUMENTASJON_TILSYNSUTGIFTER")} />
    </VStack>
  );
};

export default VilkårInnhold;
