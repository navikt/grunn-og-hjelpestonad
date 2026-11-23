import React, { useEffect, useRef, useState } from "react";
import { Accordion, BodyShort, Button, LocalAlert, Skeleton, VStack } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import type { TrygdetidResponse } from "~/api/generated/types.gen";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import {
  begrunnelseDetalj,
  PeriodeKort,
  PeriodeRedigering,
} from "~/komponenter/behandling/vilkår/felles/PeriodeKort";
import { usePerioder } from "~/komponenter/behandling/vilkår/felles/usePerioder";
import { usePeriodeEndringer } from "~/komponenter/behandling/vilkår/felles/usePeriodeEndringer";
import { hentTrygdetidPerioderFraBackend, useTrygdetidApi } from "./useTrygdetidApi";
import { FULL_TRYGDETID, kildeTekst, useTrygdetidSkjema } from "./useTrygdetidSkjema";
import { TrygdetidSkjema } from "./TrygdetidSkjema";

export const TrygdetidSeksjon: React.FC<{
  behandlingId: string;
  åpneVedFeil?: number;
}> = ({ behandlingId, åpneVedFeil }) => {
  const erLesevisning = useErLesevisning();
  const perioder = usePerioder(behandlingId, hentTrygdetidPerioderFraBackend);
  const { lagreTrygdetid, slettTrygdetid } = useTrygdetidApi();
  const skjema = useTrygdetidSkjema();
  const endringer = usePeriodeEndringer({
    perioder,
    skjema,
    lagre: lagreTrygdetid,
    slett: (trygdetidId, callbacks) =>
      slettTrygdetid.kall({ path: { behandlingId, trygdetidId } }, callbacks),
    lagrefeilTekst: "Kunne ikke lagre trygdetiden.",
  });

  const [erÅpen, settErÅpen] = useState(false);
  const [sistBehandletÅpneVedFeil, settSistBehandletÅpneVedFeil] = useState<number | undefined>();
  const [initialÅpenSatt, settInitialÅpenSatt] = useState(false);
  const seksjonsmålRef = useRef<HTMLButtonElement>(null);

  // Avledet tilstand: justeres under rendering når propen endres, ikke i effect.
  if (åpneVedFeil !== sistBehandletÅpneVedFeil) {
    settSistBehandletÅpneVedFeil(åpneVedFeil);
    if (åpneVedFeil) {
      settErÅpen(true);
    }
  }

  if (!initialÅpenSatt && !perioder.laster && perioder.data !== undefined) {
    settInitialÅpenSatt(true);
    settErÅpen(perioder.data.length === 0);
  }

  useEffect(() => {
    if (åpneVedFeil) {
      requestAnimationFrame(() => seksjonsmålRef.current?.focus());
    }
  }, [åpneVedFeil]);

  const visSkjema = !erLesevisning && skjema.erÅpent;
  const liste = perioder.data ?? [];
  const hentefeil = perioder.error
    ? feilmeldingFra(perioder.error, "Kunne ikke hente trygdetiden.")
    : undefined;

  const lagre = () => endringer.lagre({ path: { behandlingId }, body: skjema.verdier() });

  const renderPeriode = (periode: TrygdetidResponse) => {
    if (visSkjema && skjema.redigererId === periode.id) {
      return (
        <PeriodeRedigering
          key={periode.id}
          fraOgMedDato={periode.fraOgMedDato}
          tilOgMedDato={periode.tilOgMedDato}
        >
          <TrygdetidSkjema skjema={skjema} lagrer={endringer.lagrer} onLagre={lagre} />
        </PeriodeRedigering>
      );
    }

    return (
      <PeriodeKort
        key={periode.id}
        fraOgMedDato={periode.fraOgMedDato}
        tilOgMedDato={periode.tilOgMedDato}
        detaljer={[
          {
            etikett: "Trygdetidsbrøk",
            // Kilden «Ingen» betyr at stønaden ikke reduseres, ikke at søker har 40 års trygdetid.
            verdi:
              periode.kilde === "INGEN"
                ? `Ingen reduksjon (${FULL_TRYGDETID}/${FULL_TRYGDETID})`
                : `${periode.antallÅr}/${FULL_TRYGDETID}`,
          },
          { etikett: "Kilde", verdi: kildeTekst[periode.kilde] },
          ...begrunnelseDetalj(periode.begrunnelse),
        ]}
        opptatt={endringer.opptatt}
        sletter={endringer.sletterId === periode.id}
        onEndre={erLesevisning ? undefined : () => skjema.åpneRedigeringAvPeriode(periode)}
        onSlett={erLesevisning ? undefined : () => endringer.slett(periode)}
      />
    );
  };

  return (
    <Accordion.Item open={erÅpen} onOpenChange={settErÅpen}>
      <Accordion.Header id="trygdetid" ref={seksjonsmålRef}>
        Trygdetid
      </Accordion.Header>

      <Accordion.Content>
        <VStack gap="space-24">
          {hentefeil && (
            <LocalAlert status="error">
              <LocalAlert.Content>
                <BodyShort>{hentefeil}</BodyShort>
              </LocalAlert.Content>
            </LocalAlert>
          )}

          <div aria-live="polite" aria-atomic="true">
            {endringer.slettefeil && (
              <LocalAlert status="error">
                <LocalAlert.Content>
                  <BodyShort>{endringer.slettefeil}</BodyShort>
                </LocalAlert.Content>
              </LocalAlert>
            )}
          </div>

          {/* Skeleton bare ved første lasting. Ved ny henting beholdes lista som allerede vises. */}
          {perioder.laster && perioder.data === undefined && (
            <Skeleton variant="rectangle" height={120} />
          )}

          {!perioder.laster && !perioder.error && liste.length === 0 && (
            <BodyShort>Ingen trygdetid er lagt inn ennå.</BodyShort>
          )}

          {liste.map(renderPeriode)}

          {visSkjema && skjema.redigererId === null && (
            <TrygdetidSkjema skjema={skjema} lagrer={endringer.lagrer} onLagre={lagre} />
          )}

          {!erLesevisning && !skjema.erÅpent && (
            <div>
              <Button
                type="button"
                variant="secondary"
                icon={<PlusIcon aria-hidden />}
                disabled={endringer.opptatt}
                onClick={skjema.åpneNyPeriode}
              >
                Legg til trygdetid
              </Button>
            </div>
          )}
        </VStack>
      </Accordion.Content>
    </Accordion.Item>
  );
};
