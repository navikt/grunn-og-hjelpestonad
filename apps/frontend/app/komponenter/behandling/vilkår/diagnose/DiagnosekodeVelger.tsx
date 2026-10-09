import React, { useState } from "react";
import { UNSAFE_Combobox } from "@navikt/ds-react";
import type { DiagnosekodeResponse } from "~/types/vilkår";
import { useDiagnosekodeSøk } from "./useDiagnosekodeSøk";
import { visningAvDiagnosekode } from "./diagnosekode";

interface Props {
  id: string;
  valgt: DiagnosekodeResponse | undefined;
  låst: boolean;
  feil: string | undefined;
  onValgtEndret: (diagnosekode: DiagnosekodeResponse | undefined) => void;
  onBlur: () => void;
}

function tilOption(diagnosekode: DiagnosekodeResponse) {
  return { label: visningAvDiagnosekode(diagnosekode), value: diagnosekode.kode };
}

export const DiagnosekodeVelger: React.FC<Props> = ({
  id,
  valgt,
  låst,
  feil,
  onValgtEndret,
  onBlur,
}) => {
  const [søketekst, settSøketekst] = useState("");
  const søk = useDiagnosekodeSøk(søketekst);

  const treff = søk.treff.map(tilOption);
  const valgte = valgt ? [tilOption(valgt)] : [];
  const valgtManglerITreff = valgt !== undefined && !søk.treff.some((d) => d.kode === valgt.kode);

  return (
    <UNSAFE_Combobox
      id={id}
      label="Diagnose (ICD-10)"
      description={
        låst
          ? "Diagnosen er valgt fra diagnosegruppen."
          : "Søk på diagnosekode eller diagnosenavn."
      }
      options={valgtManglerITreff ? [...valgte, ...treff] : treff}
      filteredOptions={søketekst.trim() === "" ? valgte : treff}
      selectedOptions={valgte}
      isLoading={søk.laster}
      readOnly={låst}
      onChange={settSøketekst}
      onToggleSelected={(kode, erValgt) =>
        onValgtEndret(erValgt ? søk.treff.find((d) => d.kode === kode) : undefined)
      }
      onBlur={onBlur}
      error={feil ?? (søk.feil ? "Kunne ikke søke i kodeverket. Prøv igjen." : undefined)}
    />
  );
};
