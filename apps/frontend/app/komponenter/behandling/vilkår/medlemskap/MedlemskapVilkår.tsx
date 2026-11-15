import React from "react";
import { Select } from "@navikt/ds-react";
import { useMedlemskapVilkårApi } from "./useMedlemskapVilkår";
import { regelverkTekst, vilkårSpørsmål, type Regelverk } from "~/types/vilkår";
import { begrunnelseDetalj, VilkårSeksjon } from "../felles/VilkårSeksjon";
import { VilkårPeriodeSkjema } from "../felles/VilkårPeriodeSkjema";
import { useMedlemskapVilkårSkjema } from "./useMedlemskapVilkårSkjema";
import { usePeriodeEndringer } from "../felles/usePeriodeEndringer";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useVilkårContext } from "~/komponenter/behandling/vilkår/VilkårContext";
import { MedlemskapFaktagrunnlag } from "~/komponenter/behandling/faktagrunnlag/MedlemskapFaktagrunnlag";

const ID_PREFIKS = "medlemskap";

export const MedlemskapVilkår: React.FC = () => {
  const { behandlingId } = useBehandlingContext();
  const { medlemskapsPerioder: perioder } = useVilkårContext();
  const { lagre, slett } = useMedlemskapVilkårApi();

  const skjema = useMedlemskapVilkårSkjema();
  const endringer = usePeriodeEndringer({
    perioder,
    skjema,
    lagre,
    slett: (vilkårPeriodeId, callbacks) =>
      slett.kall({ path: { behandlingId, vilkårPeriodeId } }, callbacks),
    lagrefeilTekst: "Kunne ikke lagre perioden.",
  });

  const lagrePeriode = () => {
    const felles = skjema.fellesVerdier();

    endringer.lagre({
      path: { behandlingId },
      body: {
        id: felles.id,
        regelverk: skjema.regelverk as Regelverk,
        vurdering: felles.vurdering,
        begrunnelse: felles.begrunnelse,
        fraOgMedDato: felles.fraOgMedDato,
        tilOgMedDato: felles.tilOgMedDato,
      },
    });
  };

  return (
    <VilkårSeksjon
      nøkkel="medlemskap"
      perioder={perioder}
      hentefeilTekst="Kunne ikke hente vurderinger av medlemskap"
      detaljerFor={(periode) => [
        { etikett: "Regelverk", verdi: regelverkTekst[periode.regelverk] },
        ...begrunnelseDetalj(periode.begrunnelse),
      ]}
      skjema={skjema}
      endringer={endringer}
      leggTil={{ åpne: skjema.åpneNyPeriode }}
      grunnlag={<MedlemskapFaktagrunnlag />}
    >
      <VilkårPeriodeSkjema
        idPrefiks={ID_PREFIKS}
        spørsmål={vilkårSpørsmål.medlemskap}
        skjema={skjema}
        lagrer={endringer.lagrer}
        onLagre={lagrePeriode}
        ekstraFelter={[{ felt: "regelverk", id: `${ID_PREFIKS}-regelverk` }]}
      >
        <Select
          id={`${ID_PREFIKS}-regelverk`}
          label="Regelverk"
          value={skjema.regelverk}
          onChange={(event) => {
            const verdi = event.target.value as Regelverk | "";
            skjema.endreRegelverk(verdi);
          }}
          error={skjema.skjemafeil.regelverk}
        >
          <option value="">Velg regelverk</option>
          {Object.entries(regelverkTekst).map(([verdi, tekst]) => (
            <option key={verdi} value={verdi}>
              {tekst}
            </option>
          ))}
        </Select>
      </VilkårPeriodeSkjema>
    </VilkårSeksjon>
  );
};
