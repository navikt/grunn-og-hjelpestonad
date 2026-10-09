import React from "react";
import { Checkbox } from "@navikt/ds-react";
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
import { useDiagnoseVilkårSkjema } from "./useDiagnoseVilkårSkjema";
import { DiagnosekodeVelger } from "./DiagnosekodeVelger";
import { visningAvDiagnosekode } from "./diagnosekode";
import styles from "./DiagnoseVilkår.module.css";

const ID_PREFIKS = "diagnose";

function grupperDiagnoser(
  perioder: VilkårDiagnoseResponse[] | undefined
): VilkårPeriodeGruppe<VilkårDiagnoseResponse>[] {
  const grupper = new Map<string, VilkårPeriodeGruppe<VilkårDiagnoseResponse>>();

  for (const periode of perioder ?? []) {
    const nøkkel = periode.diagnose.kode;
    const gruppe = grupper.get(nøkkel);

    if (gruppe) {
      gruppe.perioder.push(periode);
    } else {
      grupper.set(nøkkel, {
        nøkkel,
        tittel: visningAvDiagnosekode(periode.diagnose),
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
    const { diagnosekode } = skjema;
    if (!diagnosekode) return;

    lagre.kall(
      {
        path: { behandlingId },
        body: {
          id: felles.id,
          kode: diagnosekode.kode,
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
        <DiagnosekodeVelger
          id={`${ID_PREFIKS}-diagnose`}
          valgt={skjema.diagnosekode}
          låst={skjema.diagnoseErLåst}
          feil={skjema.feil.diagnose}
          onValgtEndret={skjema.velgDiagnose}
          onBlur={() => skjema.validerFelt("diagnose")}
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
