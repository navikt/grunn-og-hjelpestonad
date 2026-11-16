import React from "react";
import { Accordion, BodyShort, Button, LocalAlert, Skeleton, VStack } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import type { FellesVilkårPeriodeFelter } from "./useVilkårSkjema";
import { PeriodeKort, PeriodeRedigering, type Detalj } from "./PeriodeKort";
import { VilkårGruppeItem } from "./VilkårGruppeItem";
import type { PerioderResult } from "./usePerioder";
import type { PeriodeEndringerVisning } from "./usePeriodeEndringer";
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
  endringer,
  leggTil,
  gruppering,
  children,
}: {
  perioder: PerioderResult<T>;
  hentefeil: string | undefined;
  detaljerFor: (periode: T) => Detalj[];
  skjema: VilkårSeksjonSkjema<T>;
  endringer: PeriodeEndringerVisning<T>;
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
        <PeriodeRedigering
          key={periode.id}
          fraOgMedDato={periode.fraOgMedDato}
          tilOgMedDato={periode.tilOgMedDato}
        >
          {children}
        </PeriodeRedigering>
      );
    }

    return (
      <PeriodeKort
        key={periode.id}
        fraOgMedDato={periode.fraOgMedDato}
        tilOgMedDato={periode.tilOgMedDato}
        erVilkårOppfylt={periode.erVilkårOppfylt}
        detaljer={detaljerFor(periode)}
        opptatt={endringer.opptatt}
        onEndre={erLesevisning ? undefined : () => skjema.åpneRedigeringAvPeriode(periode)}
        onSlett={erLesevisning ? undefined : () => endringer.slett(periode)}
        sletter={endringer.sletterId === periode.id}
      />
    );
  };

  const renderGruppe = (gruppe: VilkårPeriodeGruppe<T>) => (
    <VilkårGruppeItem
      key={gruppe.nøkkel}
      tittel={gruppe.tittel}
      erOppfylt={gruppe.perioder.some((periode) => periode.erVilkårOppfylt)}
      defaultOpen={gruppering?.grupper.length === 1}
      disabled={endringer.opptatt}
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
        {endringer.slettefeil && (
          <LocalAlert status="error">
            <LocalAlert.Content>{endringer.slettefeil}</LocalAlert.Content>
          </LocalAlert>
        )}
      </div>

      {/* Skeleton bare ved første lasting. Ved ny henting beholdes lista som allerede vises. */}
      {laster && periodeliste === undefined && <Skeleton variant="rectangle" height={120} />}

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
            disabled={endringer.opptatt}
            onClick={leggTil.åpne}
          >
            {leggTil.tekst ?? "Legg til periode"}
          </Button>
        </div>
      )}
    </VStack>
  );
}
