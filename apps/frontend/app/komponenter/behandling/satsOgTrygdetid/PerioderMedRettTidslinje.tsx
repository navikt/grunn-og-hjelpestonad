import React from "react";
import { BodyShort, Box, Heading, Loader, LocalAlert, Timeline, VStack } from "@navikt/ds-react";
import { CheckmarkCircleIcon, CircleSlashIcon } from "@navikt/aksel-icons";
import type { PeriodeMedRettResponse } from "~/api/generated/types.gen";
import { usePerioderMedRett } from "./usePerioderMedRett";

const ÅPEN_ENDE_MARGIN_ÅR = 1;

type Segment = {
  id: string;
  start: Date;
  slutt: Date;
  harRett: boolean;
  fraTekst: string;
  tilTekst: string;
};

export const PerioderMedRettTidslinje: React.FC<{ behandlingId: string }> = ({ behandlingId }) => {
  const { data: perioder, error, laster } = usePerioderMedRett(behandlingId);

  return (
    <Box shadow="dialog" background="neutral-soft" padding="space-24" borderRadius="4">
      <VStack gap="space-16">
        <Heading level="2" size="small">
          Perioder med rett
        </Heading>
        <TidslinjeInnhold perioder={perioder} error={error} laster={laster} />
      </VStack>
    </Box>
  );
};

const TidslinjeInnhold: React.FC<{
  perioder?: PeriodeMedRettResponse[];
  error?: unknown;
  laster: boolean;
}> = ({ perioder, error, laster }) => {
  if (laster) {
    return <Loader size="medium" title="Henter perioder med rett" />;
  }

  if (error) {
    return (
      <LocalAlert status="error">
        <LocalAlert.Content>Kunne ikke hente perioder med rett.</LocalAlert.Content>
      </LocalAlert>
    );
  }

  const segmenter = utledSegmenter(perioder ?? []);

  if (segmenter.length === 0) {
    return (
      <BodyShort>
        Ingen perioder med rett. Fullfør vilkårsvurderingen for å utlede periodene.
      </BodyShort>
    );
  }

  return (
    <Timeline>
      <Timeline.Row label="Rett til stønad">
        {segmenter.map((segment) => (
          <Timeline.Period
            key={segment.id}
            start={segment.start}
            end={segment.slutt}
            status={segment.harRett ? "success" : "neutral"}
            statusLabel={statusTekst(segment)}
            icon={segment.harRett ? <CheckmarkCircleIcon /> : <CircleSlashIcon />}
          >
            <VStack gap="space-8">
              <BodyShort weight="semibold">{statusTekst(segment)}</BodyShort>
              <BodyShort>
                {segment.fraTekst} – {segment.tilTekst}
              </BodyShort>
            </VStack>
          </Timeline.Period>
        ))}
      </Timeline.Row>
    </Timeline>
  );
};

function statusTekst(segment: Segment): string {
  return segment.harRett ? "Rett til stønad" : "Ingen rett til stønad";
}

function utledSegmenter(perioder: PeriodeMedRettResponse[]): Segment[] {
  const vindu = utledVindu(perioder);
  const tidsrom = perioder
    .map((periode) => ({
      periode,
      start: tilDato(periode.fraOgMedDato, vindu.start),
      slutt: tilDato(periode.tilOgMedDato, vindu.slutt),
    }))
    .sort((a, b) => a.start.getTime() - b.start.getTime());

  return tidsrom.flatMap(({ periode, start, slutt }, indeks) => {
    const forrigePeriode = tidsrom[indeks - 1];
    const hull = utledHull(forrigePeriode?.slutt, start, indeks);

    const medRett: Segment = {
      id: periode.id,
      start,
      slutt,
      harRett: true,
      fraTekst: periode.fraOgMedDato ? formaterDato(start) : "Fra fødsel",
      tilTekst: periode.tilOgMedDato ? formaterDato(slutt) : "løpende",
    };

    return hull ? [hull, medRett] : [medRett];
  });
}

function utledHull(
  forrigeSlutt: Date | undefined,
  nesteStart: Date,
  indeks: number
): Segment | undefined {
  if (!forrigeSlutt) return undefined;

  const start = leggTilDager(forrigeSlutt, 1);
  const slutt = leggTilDager(nesteStart, -1);

  if (start > slutt) return undefined;

  return {
    id: `uten-rett-${indeks}`,
    start,
    slutt,
    harRett: false,
    fraTekst: formaterDato(start),
    tilTekst: formaterDato(slutt),
  };
}

function utledVindu(perioder: PeriodeMedRettResponse[]): { start: Date; slutt: Date } {
  const iDag = iDagUtenTid();
  const kjenteDatoer = perioder
    .flatMap((periode) => [periode.fraOgMedDato, periode.tilOgMedDato])
    .filter((dato): dato is string => Boolean(dato))
    .map((dato) => parseIsoDato(dato).getTime());

  const tidligste = kjenteDatoer.length > 0 ? new Date(Math.min(...kjenteDatoer)) : iDag;
  const seneste = kjenteDatoer.length > 0 ? new Date(Math.max(...kjenteDatoer)) : iDag;

  return {
    start: leggTilÅr(tidligste, -ÅPEN_ENDE_MARGIN_ÅR),
    slutt: leggTilÅr(seneste > iDag ? seneste : iDag, ÅPEN_ENDE_MARGIN_ÅR),
  };
}

function tilDato(isoDato: string | null | undefined, reserve: Date): Date {
  return isoDato ? parseIsoDato(isoDato) : reserve;
}

function parseIsoDato(isoDato: string): Date {
  const [år, måned, dag] = isoDato.split("-").map(Number);
  return new Date(år, måned - 1, dag);
}

function leggTilDager(dato: Date, dager: number): Date {
  return new Date(dato.getFullYear(), dato.getMonth(), dato.getDate() + dager);
}

function leggTilÅr(dato: Date, år: number): Date {
  return new Date(dato.getFullYear() + år, dato.getMonth(), dato.getDate());
}

function iDagUtenTid(): Date {
  const nå = new Date();
  return new Date(nå.getFullYear(), nå.getMonth(), nå.getDate());
}

function formaterDato(dato: Date): string {
  return dato.toLocaleDateString("nb-NO", { day: "2-digit", month: "2-digit", year: "numeric" });
}
