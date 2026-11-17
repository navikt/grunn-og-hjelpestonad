import React from "react";
import { Accordion, BodyShort, HGrid, HStack, Tag, VStack } from "@navikt/ds-react";
import { vilkårHjemmel, vilkårNavn, type VilkårNøkkel } from "~/types/vilkår";
import type { FellesVilkårPeriodeFelter } from "./useVilkårSkjema";
import { feilmeldingFra } from "./feilmeldingFra";
import { begrunnelseDetalj, type Detalj } from "./PeriodeKort";
import type { PerioderResult } from "./usePerioder";
import type { PeriodeEndringerVisning } from "./usePeriodeEndringer";
import { VilkårVurderinger } from "./VilkårVurderinger";

export { begrunnelseDetalj };
export type { Detalj };

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
  endringer,
  leggTil,
  gruppering,
  grunnlag,
  children,
  åpneVedFeil,
}: {
  nøkkel: VilkårNøkkel;
  perioder: PerioderResult<T>;
  hentefeilTekst: string;
  detaljerFor: (periode: T) => Detalj[];
  skjema: VilkårSeksjonSkjema<T>;
  endringer: PeriodeEndringerVisning<T>;
  leggTil: VilkårLeggTil;
  gruppering?: VilkårGruppering<T>;
  grunnlag?: React.ReactNode;
  children: React.ReactNode;
  åpneVedFeil?: number;
}) {
  const { data: periodeliste, laster } = perioder;
  const hentefeil = perioder.error ? feilmeldingFra(perioder.error, hentefeilTekst) : undefined;
  const antall = periodeliste?.length ?? 0;
  const [erÅpen, settErÅpen] = React.useState(false);
  const initialÅpenSatt = React.useRef(false);
  const seksjonsmålRef = React.useRef<HTMLButtonElement>(null);

  React.useEffect(() => {
    if (åpneVedFeil) {
      settErÅpen(true);
      requestAnimationFrame(() => seksjonsmålRef.current?.focus());
    }
  }, [åpneVedFeil]);

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
    // Under ny henting beholdes statusen fra forrige liste. «Ikke vurdert» vises bare uten data.
    if (periodeliste === undefined) {
      return { tekst: "Ikke vurdert", farge: "neutral" as const };
    }
    if (antall === 0) {
      return { tekst: "Ikke vurdert", farge: "warning" as const };
    }
    return periodeliste.some((periode) => periode.erVilkårOppfylt)
      ? { tekst: "Oppfylt", farge: "success" as const }
      : { tekst: "Ikke oppfylt", farge: "danger" as const };
  })();

  const vurderinger = (
    <VilkårVurderinger
      perioder={perioder}
      hentefeil={hentefeil}
      detaljerFor={detaljerFor}
      skjema={skjema}
      endringer={endringer}
      leggTil={leggTil}
      gruppering={gruppering}
    >
      {children}
    </VilkårVurderinger>
  );

  return (
    <Accordion.Item open={erÅpen} onOpenChange={settErÅpen}>
      <Accordion.Header id={`vilkar-${nøkkel}`} ref={seksjonsmålRef}>
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

          {grunnlag ? (
            <HGrid columns={{ xs: 1, xl: 2 }} gap="space-32" align="start">
              {grunnlag}
              {vurderinger}
            </HGrid>
          ) : (
            vurderinger
          )}
        </VStack>
      </Accordion.Content>
    </Accordion.Item>
  );
}
