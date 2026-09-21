import React from "react";
import {
  Accordion,
  BodyShort,
  Button,
  HStack,
  LocalAlert,
  Skeleton,
  Tag,
  VStack,
} from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { vilkårHjemmel, vilkårNavn, type VilkårNøkkel } from "~/types/vilkår";
import type { FellesVilkårPeriodeFelter } from "./useVilkårSkjema";
import { feilmeldingFra } from "./feilmeldingFra";
import { VilkårPeriodeKort, VilkårPeriodeRedigering, type Detalj } from "./VilkårPeriodeKort";
import { VilkårGruppeItem } from "./VilkårGruppeItem";
import type { PerioderResult } from "./usePerioder";
import type { VilkårSletting } from "./useSlettVilkårPeriode";

export type { Detalj };

export function begrunnelseDetalj(begrunnelse: string | null | undefined): Detalj[] {
  const tekst = begrunnelse?.trim();
  return tekst ? [{ etikett: "Begrunnelse", verdi: tekst }] : [];
}

export interface VilkårPeriodeGruppe<T> {
  nøkkel: string;
  tittel: string;
  perioder: T[];
}

export interface VilkårSeksjonSkjema<T> {
  erÅpent: boolean;
  redigererId: string | null;
  åpneRedigeringAvPeriode: (periode: T) => void;
}

export interface VilkårLeggTil {
  åpne: () => void;
  tekst?: string;
}

export interface VilkårGruppering<T> {
  grupper: VilkårPeriodeGruppe<T>[];
  åpenGruppe: string | undefined;
  åpneNyPeriode: (gruppe: VilkårPeriodeGruppe<T>) => void;
  className?: string;
}

export function VilkårSeksjon<T extends FellesVilkårPeriodeFelter & { erVilkårOppfylt: boolean }>({
  nøkkel,
  perioder,
  hentefeilTekst,
  detaljerFor,
  skjema,
  sletting,
  leggTil,
  gruppering,
  children,
}: {
  nøkkel: VilkårNøkkel;
  perioder: PerioderResult<T>;
  hentefeilTekst: string;
  detaljerFor: (periode: T) => Detalj[];
  skjema: VilkårSeksjonSkjema<T>;
  sletting: VilkårSletting<T>;
  leggTil: VilkårLeggTil;
  gruppering?: VilkårGruppering<T>;
  children: React.ReactNode;
}) {
  const erLesevisning = useErLesevisning();
  const { data: periodeliste, laster } = perioder;
  const hentefeil = perioder.error ? feilmeldingFra(perioder.error, hentefeilTekst) : undefined;
  const antall = periodeliste?.length ?? 0;
  const [erÅpen, settErÅpen] = React.useState(false);
  const initialÅpenSatt = React.useRef(false);

  React.useEffect(() => {
    if (initialÅpenSatt.current || laster || periodeliste === undefined) {
      return;
    }

    initialÅpenSatt.current = true;
    settErÅpen(periodeliste.length === 0);
  }, [laster, periodeliste]);

  const status = (() => {
    if (hentefeil) {
      return { tekst: "Ikke vurdert", farge: "warning" as const };
    }
    if (laster || periodeliste === undefined) {
      return { tekst: "Ikke vurdert", farge: "neutral" as const };
    }
    if (antall === 0) {
      return { tekst: "Ikke vurdert", farge: "warning" as const };
    }
    return periodeliste.some((periode) => periode.erVilkårOppfylt)
      ? { tekst: "Oppfylt", farge: "success" as const }
      : { tekst: "Ikke oppfylt", farge: "danger" as const };
  })();

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

  const visNyttSkjemaIGruppe = (gruppe: VilkårPeriodeGruppe<T>) =>
    !erLesevisning &&
    skjema.erÅpent &&
    skjema.redigererId === null &&
    gruppering?.åpenGruppe === gruppe.nøkkel;

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
      {visNyttSkjemaIGruppe(gruppe) && children}
    </VilkårGruppeItem>
  );

  return (
    <Accordion.Item open={erÅpen} onOpenChange={settErÅpen}>
      <Accordion.Header id={`vilkar-${nøkkel}`}>
        <HStack gap="space-12" align="center" wrap={false}>
          <span>{vilkårNavn[nøkkel]}</span>
          <Tag variant="strong" size="small" data-color={status.farge}>
            {status.tekst}
          </Tag>
        </HStack>
      </Accordion.Header>

      <Accordion.Content>
        <VStack gap="space-24">
          <BodyShort size="small" textColor="subtle">
            {vilkårHjemmel[nøkkel]}
          </BodyShort>

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

          {!erLesevisning &&
            skjema.erÅpent &&
            skjema.redigererId === null &&
            (!gruppering || gruppering.åpenGruppe === undefined) &&
            children}

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
      </Accordion.Content>
    </Accordion.Item>
  );
}
