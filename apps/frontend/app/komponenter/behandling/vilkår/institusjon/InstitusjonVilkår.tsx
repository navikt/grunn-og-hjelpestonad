import React from "react";
import { Select } from "@navikt/ds-react";
import { feilmeldingFra } from "../felles/feilmeldingFra";
import { useInstitusjonVilkårApi } from "./useInstitusjonVilkår";
import { useInstitusjonVilkårSkjema } from "./useInstitusjonVilkårSkjema";
import {
  oppholdstypeTekst,
  unntakshjemmelTekst,
  vilkårSpørsmål,
  Vurdering,
  type Oppholdstype,
  type Unntakshjemmel,
  type VilkårInstitusjonResponse,
} from "~/types/vilkår";
import { begrunnelseDetalj, VilkårSeksjon, type Detalj } from "../felles/VilkårSeksjon";
import { VilkårPeriodeSkjema } from "../felles/VilkårPeriodeSkjema";
import { useSlettVilkårPeriode } from "../felles/useSlettVilkårPeriode";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useVilkårContext } from "~/komponenter/behandling/vilkår/VilkårContext";

const ID_PREFIKS = "institusjon";

function institusjonDetaljer(periode: VilkårInstitusjonResponse): Detalj[] {
  if (periode.vurdering === Vurdering.JA) {
    return begrunnelseDetalj(periode.begrunnelse);
  }

  return [
    {
      etikett: "Oppholdstype",
      verdi: periode.oppholdstype ? oppholdstypeTekst[periode.oppholdstype] : "Ikke oppgitt",
    },
    {
      etikett: "Unntakshjemmel",
      verdi: periode.unntakshjemmel ? unntakshjemmelTekst[periode.unntakshjemmel] : "Ikke oppgitt",
    },
    ...begrunnelseDetalj(periode.begrunnelse),
  ];
}

export const InstitusjonVilkår: React.FC = () => {
  const { behandlingId } = useBehandlingContext();
  const { institusjonsPerioder: perioder } = useVilkårContext();
  const { lagre, slett } = useInstitusjonVilkårApi();

  const skjema = useInstitusjonVilkårSkjema();
  const sletting = useSlettVilkårPeriode(
    (vilkårPeriodeId, callbacks) =>
      slett.kall({ path: { behandlingId, vilkårPeriodeId } }, callbacks),
    perioder
  );

  const lagrePeriode = () => {
    const felles = skjema.fellesVerdier();
    const oppholdErRelevant = felles.vurdering === Vurdering.NEI;

    lagre.kall(
      {
        path: { behandlingId },
        body: {
          id: felles.id,
          oppholdstype:
            oppholdErRelevant && skjema.oppholdstype !== "" ? skjema.oppholdstype : null,
          unntakshjemmel:
            !oppholdErRelevant || skjema.oppholdstype === "" || skjema.unntakshjemmel === ""
              ? null
              : skjema.unntakshjemmel,
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
          perioder.hentPåNytt();
        },
        onError: (error) =>
          skjema.settLagringsfeil(feilmeldingFra(error, "Kunne ikke lagre perioden.")),
      }
    );
  };

  return (
    <VilkårSeksjon
      nøkkel="institusjon"
      perioder={perioder}
      hentefeilTekst="Kunne ikke hente vurderinger av institusjonsopphold"
      detaljerFor={institusjonDetaljer}
      skjema={skjema}
      sletting={sletting}
      leggTil={{ åpne: skjema.åpneNyPeriode }}
    >
      <VilkårPeriodeSkjema
        idPrefiks={ID_PREFIKS}
        spørsmål={vilkårSpørsmål.institusjon}
        skjema={skjema}
        lagrer={lagre.laster}
        onLagre={lagrePeriode}
        onVurderingEndret={skjema.endreVurdering}
      >
        {skjema.vurdering !== Vurdering.JA && (
          <>
            <Select
              id={`${ID_PREFIKS}-oppholdstype`}
              label="Oppholdstype"
              description="Fylles ut når bruker oppholder seg i institusjon eller lovregulert boform."
              value={skjema.oppholdstype}
              onChange={(event) =>
                skjema.endreOppholdstype(event.target.value as Oppholdstype | "")
              }
            >
              <option value="">Ikke aktuelt</option>
              {Object.entries(oppholdstypeTekst).map(([verdi, tekst]) => (
                <option key={verdi} value={verdi}>
                  {tekst}
                </option>
              ))}
            </Select>

            {skjema.oppholdstype !== "" && (
              <Select
                id={`${ID_PREFIKS}-unntakshjemmel`}
                label="Unntakshjemmel"
                value={skjema.unntakshjemmel}
                onChange={(event) =>
                  skjema.settUnntakshjemmel(event.target.value as Unntakshjemmel | "")
                }
              >
                <option value="">Ingen unntakshjemmel</option>
                {Object.entries(unntakshjemmelTekst).map(([verdi, tekst]) => (
                  <option key={verdi} value={verdi}>
                    {tekst}
                  </option>
                ))}
              </Select>
            )}
          </>
        )}
      </VilkårPeriodeSkjema>
    </VilkårSeksjon>
  );
};
