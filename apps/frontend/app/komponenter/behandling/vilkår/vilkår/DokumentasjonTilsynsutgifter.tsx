import React from "react";
import VilkårKomponent from "./VilkårKomponent";

const DOKUMENTASJON_TILSYNUTGIFTER_INNHOLD = {
  navn: "Dokumentasjon tilsynsutgifter",
  beskrivelse: [
    "Hvis bruker får kontantstøtte, skal dette beløpet trekkes fra den dokumenterte utgiften til barnepass.",
  ],
  valgSpørsmål: "Har brukeren dokumentert tilsynsutgifter?",
};

export const DokumentasjonTilsynsutgifter: React.FC<{
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
      navn={DOKUMENTASJON_TILSYNUTGIFTER_INNHOLD.navn}
      beskrivelse={DOKUMENTASJON_TILSYNUTGIFTER_INNHOLD.beskrivelse}
      valgSpørsmål={DOKUMENTASJON_TILSYNUTGIFTER_INNHOLD.valgSpørsmål}
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

export default DokumentasjonTilsynsutgifter;
