import React from "react";
import { BodyShort, Button, LocalAlert } from "@navikt/ds-react";
import { hjemmelbokstavTekst, satsTekst } from "~/types/vilkår";
import {
  begrunnelseDetalj,
  VilkårSeksjon,
} from "~/komponenter/behandling/vilkår/felles/VilkårSeksjon";
import { usePeriodeEndringer } from "~/komponenter/behandling/vilkår/felles/usePeriodeEndringer";
import { usePerioder } from "~/komponenter/behandling/vilkår/felles/usePerioder";
import { hentDiagnosePerioderFraBackend } from "~/komponenter/behandling/vilkår/diagnose/useDiagnoseVilkår";
import {
  hentEkstrautgifterPerioderFraBackend,
  useEkstrautgifterVilkårApi,
} from "./useEkstrautgifterVilkår";
import { useEkstrautgifterVilkårSkjema } from "./useEkstrautgifterVilkårSkjema";
import { EkstrautgifterSkjema } from "./EkstrautgifterSkjema";
import { hentUnikeDiagnoserVurdertTilJa, lagDiagnoseVisning } from "./diagnoser";

export const EkstrautgifterVilkår: React.FC<{
  behandlingId: string;
  åpneVedFeil?: number;
}> = ({ behandlingId, åpneVedFeil }) => {
  const ekstrautgiftPerioder = usePerioder(behandlingId, hentEkstrautgifterPerioderFraBackend);
  const diagnosePerioder = usePerioder(behandlingId, hentDiagnosePerioderFraBackend);
  const { lagreEkstrautgifter, slettEkstrautgifter } = useEkstrautgifterVilkårApi();

  const diagnoser = hentUnikeDiagnoserVurdertTilJa(diagnosePerioder.data);
  const visDiagnose = lagDiagnoseVisning(diagnosePerioder.data);

  const skjema = useEkstrautgifterVilkårSkjema(diagnoser);
  const endringer = usePeriodeEndringer({
    perioder: ekstrautgiftPerioder,
    skjema,
    lagre: lagreEkstrautgifter,
    slett: (vilkårPeriodeId, callbacks) =>
      slettEkstrautgifter.kall({ path: { behandlingId, vilkårPeriodeId } }, callbacks),
    lagrefeilTekst: "Kunne ikke lagre perioden.",
  });

  const lagrePeriode = () => {
    const felles = skjema.fellesVerdier();
    const { sats, ekstrautgifter } = skjema.verdier();

    endringer.lagre({
      path: { behandlingId },
      body: {
        id: felles.id,
        vurdering: felles.vurdering,
        sats,
        ekstrautgifter,
        begrunnelse: felles.begrunnelse,
        fraOgMedDato: felles.fraOgMedDato,
        tilOgMedDato: felles.tilOgMedDato,
      },
    });
  };

  const henterDiagnoser = diagnosePerioder.laster || Boolean(diagnosePerioder.error);
  const diagnoseMelding = diagnosePerioder.laster ? (
    <BodyShort>Laster diagnoser ...</BodyShort>
  ) : diagnosePerioder.error ? (
    <LocalAlert status="error">
      <LocalAlert.Content>
        <BodyShort>
          Kunne ikke hente diagnosene. Ekstrautgifter kan ikke vurderes uten dem.
        </BodyShort>
        <Button
          type="button"
          variant="secondary"
          size="small"
          onClick={() => diagnosePerioder.hentPåNytt()}
        >
          Prøv igjen
        </Button>
      </LocalAlert.Content>
    </LocalAlert>
  ) : null;

  return (
    <VilkårSeksjon
      nøkkel="ekstrautgifter"
      perioder={ekstrautgiftPerioder}
      hentefeilTekst="Kunne ikke hente vurderinger av nødvendige ekstrautgifter"
      detaljerFor={(periode) => [
        ...(periode.sats ? [{ etikett: "Sats", verdi: satsTekst[periode.sats] }] : []),
        ...periode.ekstrautgifter.map((ekstrautgift) => ({
          etikett: hjemmelbokstavTekst[ekstrautgift.hjemmelbokstav],
          verdi: ekstrautgift.diagnosekoder.map(visDiagnose).join(", "),
        })),
        ...begrunnelseDetalj(periode.begrunnelse),
      ]}
      skjema={skjema}
      endringer={endringer}
      leggTil={{ tekst: "Legg til periode", åpne: skjema.åpneNyPeriode }}
      åpneVedFeil={åpneVedFeil}
    >
      <div aria-live="polite">{diagnoseMelding}</div>
      {henterDiagnoser && (
        <div>
          <Button type="button" variant="secondary" onClick={skjema.lukk}>
            Avbryt
          </Button>
        </div>
      )}
      {!henterDiagnoser && (
        <EkstrautgifterSkjema
          skjema={skjema}
          diagnoserVurdertTilJa={diagnoser}
          lagrer={endringer.lagrer}
          onLagre={lagrePeriode}
        />
      )}
    </VilkårSeksjon>
  );
};
