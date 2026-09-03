import React from "react";
import VilkårKomponent from "./VilkårKomponent";

const ALDER_PÅ_BARN_INNHOLD = {
  navn: "Alder på barn",
  beskrivelse: [
    "Stønad kan ytes til barnet har fullført 4. skoleår. For barn som har fullført 4. skoleår, kan det gis stønad til tilsyn når barnet må ha vesentlig mer tilsyn enn det som er vanlig for jevnaldrende, eller når medlemmet på grunn av sitt arbeid må være borte fra hjemmet i lengre perioder eller på andre tidspunkter enn det en vanlig arbeidsdag medfører.",
  ],
  valgSpørsmål: "Er vilkåret om barnets alder oppfylt?",
};

export const AlderPåBarn: React.FC<{
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
      navn={ALDER_PÅ_BARN_INNHOLD.navn}
      beskrivelse={ALDER_PÅ_BARN_INNHOLD.beskrivelse}
      valgSpørsmål={ALDER_PÅ_BARN_INNHOLD.valgSpørsmål}
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

export default AlderPåBarn;
