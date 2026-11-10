import React from "react";
import { hjemmelbokstavTekst, satsTekst } from "~/types/vilkår";
import {
  begrunnelseDetalj,
  VilkårSeksjon,
} from "~/komponenter/behandling/vilkår/felles/VilkårSeksjon";
import { useSlettVilkårPeriode } from "~/komponenter/behandling/vilkår/felles/useSlettVilkårPeriode";
import { usePerioder } from "~/komponenter/behandling/vilkår/felles/usePerioder";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import { hentDiagnosePerioderFraBackend } from "~/komponenter/behandling/vilkår/diagnose/useDiagnoseVilkår";
import {
  hentEkstrautgifterPerioderFraBackend,
  useEkstrautgifterVilkårApi,
} from "./useEkstrautgifterVilkår";
import { useEkstrautgifterVilkårSkjema } from "./useEkstrautgifterVilkårSkjema";
import { EkstrautgifterSkjema } from "./EkstrautgifterSkjema";
import { hentUnikeDiagnoserVurdertTilJa, lagDiagnoseVisning } from "./diagnoser";

export const EkstrautgifterVilkår: React.FC<{ behandlingId: string }> = ({ behandlingId }) => {
  const ekstrautgiftPerioder = usePerioder(behandlingId, hentEkstrautgifterPerioderFraBackend);
  const diagnosePerioder = usePerioder(behandlingId, hentDiagnosePerioderFraBackend);
  const { lagreEkstrautgifter, slettEkstrautgifter } = useEkstrautgifterVilkårApi();

  const diagnoser = hentUnikeDiagnoserVurdertTilJa(diagnosePerioder.data);
  const visDiagnose = lagDiagnoseVisning(diagnosePerioder.data);

  const skjema = useEkstrautgifterVilkårSkjema(diagnoser);
  const sletting = useSlettVilkårPeriode(
    (vilkårPeriodeId, callbacks) =>
      slettEkstrautgifter.kall({ path: { behandlingId, vilkårPeriodeId } }, callbacks),
    ekstrautgiftPerioder
  );

  const lagrePeriode = () => {
    const felles = skjema.fellesVerdier();
    const { sats, ekstrautgifter } = skjema.verdier();

    lagreEkstrautgifter.kall(
      {
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
      },
      {
        onSuccess: () => {
          sletting.nullstillFeil();
          skjema.lukk();
          ekstrautgiftPerioder.hentPåNytt();
        },
        onError: (error) =>
          skjema.settLagringsfeil(feilmeldingFra(error, "Kunne ikke lagre perioden.")),
      }
    );
  };

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
      sletting={sletting}
      leggTil={{ tekst: "Legg til periode", åpne: skjema.åpneNyPeriode }}
    >
      <EkstrautgifterSkjema
        skjema={skjema}
        diagnoserVurdertTilJa={diagnoser}
        lagrer={lagreEkstrautgifter.laster}
        onLagre={lagrePeriode}
      />
    </VilkårSeksjon>
  );
};
