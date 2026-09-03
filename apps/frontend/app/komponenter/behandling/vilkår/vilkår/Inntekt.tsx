import React from "react";
import VilkårKomponent from "./VilkårKomponent";

const INNTEKT_INNHOLD = {
  navn: "Inntekt",
  beskrivelse: [
    "Det gis ikke stønad når den pensjonsgivende inntekten er større enn 6 ganger grunnbeløpet.",
    "Hvis bruker får gjenlevendepensjon eller omstillingsstønad, må du vurdere om inntekten er i samsvar med det som er lagt til grunn ved beregningen av stønaden.",
  ],
  valgSpørsmål: "Har brukeren inntekt under 6 ganger grunnbeløpet?",
};

export const Inntekt: React.FC<{
  spørsmålSvar: string;
  settSpørsmålSvar: (val: string) => void;
  begrunnelse: string;
  settBegrunnelse: (val: string) => void;
  låst: boolean;
  settLåst: (val: boolean) => void;
  lagrer: boolean;
  onLagre: () => Promise<boolean>;
  onSlett: () => void;
}> = ({
  begrunnelse,
  settBegrunnelse,
  spørsmålSvar,
  settSpørsmålSvar,
  låst,
  settLåst,
  lagrer,
  onLagre,
  onSlett,
}) => {
  return (
    <VilkårKomponent
      navn={INNTEKT_INNHOLD.navn}
      beskrivelse={INNTEKT_INNHOLD.beskrivelse}
      valgSpørsmål={INNTEKT_INNHOLD.valgSpørsmål}
      spørsmålSvar={spørsmålSvar}
      onChangeSpørsmål={settSpørsmålSvar}
      begrunnelse={begrunnelse}
      onChangeBegrunnelse={settBegrunnelse}
      låst={låst}
      settLåst={settLåst}
      lagrer={lagrer}
      onLagre={onLagre}
      onSlett={onSlett}
    />
  );
};

export default Inntekt;
