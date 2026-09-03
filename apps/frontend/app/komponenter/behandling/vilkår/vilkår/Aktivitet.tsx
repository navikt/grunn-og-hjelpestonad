import React from "react";
import VilkårKomponent from "./VilkårKomponent";

const AKTIVITET_INNHOLD = {
  navn: "Aktivitet",
  beskrivelse: [
    "Hvis bruker er i utdanning eller er arbeidssøker, overføres saken til de som jobber med tilleggsstønader.",
  ],
  valgSpørsmål: "Er brukeren i arbeid, etablerer egen virksomhet eller har en forbigående sykdom?",
};

export const Aktivitet: React.FC<{
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
      navn={AKTIVITET_INNHOLD.navn}
      beskrivelse={AKTIVITET_INNHOLD.beskrivelse}
      valgSpørsmål={AKTIVITET_INNHOLD.valgSpørsmål}
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

export default Aktivitet;
