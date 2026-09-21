import React from "react";
import { Checkbox, TextField } from "@navikt/ds-react";
import { feilmeldingFra } from "../felles/feilmeldingFra";
import { useDiagnoseVilkårApi } from "./useDiagnoseVilkår";
import { vilkårSpørsmål, type VilkårDiagnoseResponse } from "~/types/vilkår";
import {
  begrunnelseDetalj,
  VilkårSeksjon,
  type VilkårPeriodeGruppe,
} from "../felles/VilkårSeksjon";
import { VilkårPeriodeSkjema } from "../felles/VilkårPeriodeSkjema";
import { useSlettVilkårPeriode } from "../felles/useSlettVilkårPeriode";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useVilkårContext } from "~/komponenter/behandling/vilkår/VilkårContext";
import { diagnoseNøkkel, useDiagnoseVilkårSkjema } from "./useDiagnoseVilkårSkjema";
import styles from "./DiagnoseVilkår.module.css";

const ID_PREFIKS = "diagnose";

function grupperDiagnoser(
  perioder: VilkårDiagnoseResponse[] | undefined
): VilkårPeriodeGruppe<VilkårDiagnoseResponse>[] {
  const grupper = new Map<string, VilkårPeriodeGruppe<VilkårDiagnoseResponse>>();

  for (const periode of perioder ?? []) {
    const nøkkel = diagnoseNøkkel(periode.diagnose);
    const gruppe = grupper.get(nøkkel);

    if (gruppe) {
      gruppe.perioder.push(periode);
    } else {
      grupper.set(nøkkel, {
        nøkkel,
        tittel: periode.diagnose,
        perioder: [periode],
      });
    }
  }

  return Array.from(grupper.values());
}

export const DiagnoseVilkår: React.FC = () => {
  const { behandlingId } = useBehandlingContext();
  const { diagnosePerioder } = useVilkårContext();
  const { lagre, slett } = useDiagnoseVilkårApi();

  const diagnoseGrupper = grupperDiagnoser(diagnosePerioder.data);

  const skjema = useDiagnoseVilkårSkjema();
  const sletting = useSlettVilkårPeriode(
    (vilkårPeriodeId, callbacks) =>
      slett.kall({ path: { behandlingId, vilkårPeriodeId } }, callbacks),
    diagnosePerioder
  );

  const lagrePeriode = () => {
    const felles = skjema.fellesVerdier();

    lagre.kall(
      {
        path: { behandlingId },
        body: {
          id: felles.id,
          diagnose: skjema.diagnose.trim(),
          erYrkesskade: skjema.erYrkesskade,
          vurdering: felles.vurdering,
          begrunnelse: felles.begrunnelse,
          fraOgMedDato: felles.fraOgMedDato,
          tilOgMedDato: felles.tilOgMedDato,
        },
      },
      {
        onSuccess: () => {
          sletting.nullstillFeil();
          skjema.lukk();
          diagnosePerioder.hentPåNytt();
        },
        onError: (error) =>
          skjema.settLagringsfeil(feilmeldingFra(error, "Kunne ikke lagre perioden.")),
      }
    );
  };

  return (
    <VilkårSeksjon
      nøkkel="diagnose"
      perioder={diagnosePerioder}
      hentefeilTekst="Kunne ikke hente vurderinger av diagnose"
      detaljerFor={(periode) => [
        { etikett: "Yrkesskade", verdi: periode.erYrkesskade ? "Ja" : "Nei" },
        ...begrunnelseDetalj(periode.begrunnelse),
      ]}
      skjema={skjema}
      sletting={sletting}
      leggTil={{ tekst: "Legg til diagnose", åpne: skjema.åpneNyDiagnose }}
      gruppering={{
        grupper: diagnoseGrupper,
        className: styles.diagnoseAccordion,
        åpenGruppe: skjema.åpenGruppe,
        åpneNyPeriode: skjema.åpneNyPeriodeForDiagnose,
      }}
    >
      <VilkårPeriodeSkjema
        idPrefiks={ID_PREFIKS}
        spørsmål={vilkårSpørsmål.diagnose}
        skjema={skjema}
        lagrer={lagre.laster}
        onLagre={lagrePeriode}
        ekstraFelter={[{ felt: "diagnose", id: `${ID_PREFIKS}-diagnose` }]}
      >
        <TextField
          id={`${ID_PREFIKS}-diagnose`}
          label="Diagnose"
          description={
            skjema.diagnoseErLåst ? "Diagnosen er valgt fra diagnosegruppen." : undefined
          }
          value={skjema.diagnose}
          readOnly={skjema.diagnoseErLåst}
          onChange={(event) => {
            skjema.settDiagnose(event.target.value);
          }}
          onBlur={() => skjema.validerFelt("diagnose")}
          error={skjema.feil.diagnose}
          autoComplete="off"
        />
        <Checkbox
          checked={skjema.erYrkesskade}
          onChange={(event) => skjema.settErYrkesskade(event.target.checked)}
        >
          Sykdommen eller skaden skyldes yrkesskade
        </Checkbox>
      </VilkårPeriodeSkjema>
    </VilkårSeksjon>
  );
};
