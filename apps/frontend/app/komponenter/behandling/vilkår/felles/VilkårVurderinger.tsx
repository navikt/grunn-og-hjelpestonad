import React from "react";
import { Accordion, BodyShort, Button, LocalAlert, Skeleton, VStack } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import type { FellesVilkårPeriodeFelter } from "./useVilkårSkjema";
import { VilkårPeriodeKort, VilkårPeriodeRedigering, type Detalj } from "./VilkårPeriodeKort";
import { VilkårGruppeItem } from "./VilkårGruppeItem";
import type { PerioderResult } from "./usePerioder";
import type { VilkårSletting } from "./useSlettVilkårPeriode";
import type {
  VilkårGruppering,
  VilkårLeggTil,
  VilkårPeriodeGruppe,
  VilkårSeksjonSkjema,
} from "./VilkårSeksjon";

export function VilkårVurderinger<
  T extends FellesVilkårPeriodeFelter & { erVilkårOppfylt: boolean },
>({
  perioder,
  hentefeil,
  detaljerFor,
  skjema,
  sletting,
  leggTil,
  gruppering,
  children,
}: {
  perioder: PerioderResult<T>;
  hentefeil: string | undefined;
  detaljerFor: (periode: T) => Detalj[];
  skjema: VilkårSeksjonSkjema<T>;
  sletting: VilkårSletting<T>;
  leggTil: VilkårLeggTil;
  gruppering?: VilkårGruppering<T>;
  children: React.ReactNode;
}) {
  const erLesevisning = useErLesevisning();
  const { data: periodeliste, laster } = perioder;
  const antall = periodeliste?.length ?? 0;

  const visNyttSkjema = (gruppenøkkel: string | undefined) =>
    !erLesevisning &&
    skjema.erÅpent &&
    skjema.redigererId === null &&
    gruppering?.åpenGruppe === gruppenøkkel;

  const renderPeriode = (periode: T) => {
    if (!erLesevisning && skjema.erÅpent && skjema.redigererId === periode.id) {
      return (
        <VilkårPeriodeRedigering
          key={periode.id}
          fraOgMedDato={periode.fraOgMedDato}
          tilOgMedDato={periode.tilOgMedDato}
        >
          {children}
        </VilkårPeriodeRedigering>
      );
    }

    return (
      <VilkårPeriodeKort
        key={periode.id}
        fraOgMedDato={periode.fraOgMedDato}
        tilOgMedDato={periode.tilOgMedDato}
        erVilkårOppfylt={periode.erVilkårOppfylt}
        detaljer={detaljerFor(periode)}
        onEndre={erLesevisning ? undefined : () => skjema.åpneRedigeringAvPeriode(periode)}
        onSlett={erLesevisning ? undefined : () => sletting.slett(periode)}
        sletter={sletting.sletterId === periode.id}
      />
    );
  };

  const renderGruppe = (gruppe: VilkårPeriodeGruppe<T>) => (
    <VilkårGruppeItem
      key={gruppe.nøkkel}
      tittel={gruppe.tittel}
      erOppfylt={gruppe.perioder.some((periode) => periode.erVilkårOppfylt)}
      defaultOpen={gruppering?.grupper.length === 1}
      onLeggTil={
        erLesevisning || skjema.erÅpent ? undefined : () => gruppering?.åpneNyPeriode(gruppe)
      }
    >
      {gruppe.perioder.map(renderPeriode)}
      {visNyttSkjema(gruppe.nøkkel) && children}
    </VilkårGruppeItem>
  );

  return (
    <VStack gap="space-24">
      {hentefeil && (
        <LocalAlert status="error">
          <LocalAlert.Content>{hentefeil}</LocalAlert.Content>
        </LocalAlert>
      )}

      <div aria-live="polite" aria-atomic="true">
        {sletting.feil && (
          <LocalAlert status="error">
            <LocalAlert.Content>{sletting.feil}</LocalAlert.Content>
          </LocalAlert>
        )}
      </div>

      {laster && <Skeleton variant="rectangle" height={120} />}

      {!laster && !hentefeil && antall === 0 && (
        <BodyShort>Ingen perioder er vurdert for dette vilkåret ennå.</BodyShort>
      )}

      {gruppering && antall > 0 ? (
        <Accordion className={gruppering.className}>
          {gruppering.grupper.map(renderGruppe)}
        </Accordion>
      ) : (
        periodeliste?.map(renderPeriode)
      )}

      {visNyttSkjema(undefined) && children}

      {!erLesevisning && !skjema.erÅpent && (
        <div>
          <Button
            type="button"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            onClick={leggTil.åpne}
          >
            {leggTil.tekst ?? "Legg til periode"}
          </Button>
        </div>
      )}
    </VStack>
  );
}
