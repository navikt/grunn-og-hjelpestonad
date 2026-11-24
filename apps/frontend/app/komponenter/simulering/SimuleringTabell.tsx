import { Box, BodyShort, Detail, Heading, HStack, Table, Tag, VStack } from "@navikt/ds-react";
import React from "react";
import type { Simuleringsbeløp, Simuleringsperiode } from "~/api/generated/types.gen";
import { formaterBelop } from "~/utils/utils";
import {
  erEtterbetaling,
  erFeilutbetaling,
  formaterAntallMåneder,
  formaterBeløpPerMåned,
  formaterPeriode,
  harManuellPostering,
  harManuellPosteringIPerioden,
} from "./formaterSimulering";

type Beløpskolonne = {
  nøkkel: keyof Simuleringsbeløp;
  tittel: string;
};

/**
 * Som i ba-sak. Feilutbetaling og etterbetaling vises ikke per periode: Resultat har samme beløp med fortegn, taggene
 * viser hvilken type det er, og totalene står i oppsummeringen.
 */
const BELØPSKOLONNER: Beløpskolonne[] = [
  { nøkkel: "nyttBeløp", tittel: "Nytt beløp" },
  { nøkkel: "tidligereUtbetalt", tittel: "Tidligere utbetalt" },
  { nøkkel: "resultat", tittel: "Resultat" },
];

const MANUELL_POSTERING_KOLONNE: Beløpskolonne = {
  nøkkel: "manuellPostering",
  tittel: "Manuell postering",
};

/** Står sist i raden. For en periode på én måned er den lik Resultat. */
const RESULTAT_FOR_PERIODEN_TITTEL = "Resultat for perioden";

function Beløpscelle({ tittel, beløpstekst }: { tittel: string; beløpstekst: string }) {
  return (
    <Table.DataCell align="right">
      <VStack gap="space-4" align="end">
        <Detail textColor="subtle">{tittel}</Detail>
        <BodyShort size="small">{beløpstekst}</BodyShort>
      </VStack>
    </Table.DataCell>
  );
}

function SimuleringsperiodeRad({
  periode,
  beløpskolonner,
}: {
  periode: Simuleringsperiode;
  beløpskolonner: Beløpskolonne[];
}) {
  const periodeTekst = formaterPeriode(periode.fom, periode.tom, periode.antallMåneder);
  const flereMåneder = periode.antallMåneder > 1;

  return (
    <Table.Row>
      <Table.HeaderCell scope="row">
        {/* Taggene står til høyre for datoen når det er plass, og brytes under ellers. */}
        <HStack gap="space-8" align="start" wrap>
          <VStack gap="space-4" align="start">
            {periodeTekst}
            {flereMåneder && <Detail>{formaterAntallMåneder(periode.antallMåneder)}</Detail>}
          </VStack>
          <HStack gap="space-4" wrap>
            {periode.erNesteUtbetaling && (
              <Tag size="small" variant="moderate" data-color="info">
                Neste utbetaling
              </Tag>
            )}
            {erEtterbetaling(periode) && (
              <Tag size="small" variant="moderate" data-color="success">
                Etterbetaling
              </Tag>
            )}
            {erFeilutbetaling(periode) && (
              <Tag size="small" variant="moderate" data-color="warning">
                Feilutbetaling
              </Tag>
            )}
            {harManuellPosteringIPerioden(periode) && (
              <Tag size="small" variant="strong" data-color="danger">
                Manuell postering
              </Tag>
            )}
          </HStack>
        </HStack>
      </Table.HeaderCell>
      {beløpskolonner.map((kolonne) => {
        // Manuell postering vises bare i periodene som har det. Tom celle ellers, så kolonnene står likt.
        if (kolonne.nøkkel === "manuellPostering" && !harManuellPosteringIPerioden(periode)) {
          return <Table.DataCell key={kolonne.nøkkel} />;
        }
        const beløpPerMåned = periode.beløpPerMåned[kolonne.nøkkel];
        return (
          <Beløpscelle
            key={kolonne.nøkkel}
            tittel={kolonne.tittel}
            beløpstekst={
              flereMåneder ? formaterBeløpPerMåned(beløpPerMåned) : formaterBelop(beløpPerMåned)
            }
          />
        );
      })}
      <Beløpscelle
        tittel={RESULTAT_FOR_PERIODEN_TITTEL}
        beløpstekst={formaterBelop(periode.sumForPerioden.resultat)}
      />
    </Table.Row>
  );
}

export function SimuleringTabell({ perioder }: { perioder: Simuleringsperiode[] }) {
  const visManuellPostering = harManuellPostering(perioder);
  const beløpskolonner = visManuellPostering
    ? [...BELØPSKOLONNER, MANUELL_POSTERING_KOLONNE]
    : BELØPSKOLONNER;
  // Backend sorterer eldste først. Saksbehandler vil se siste periode øverst.
  const perioderMedSisteØverst = [...perioder].reverse();

  return (
    <VStack gap="space-8">
      <Heading level="3" size="small">
        Perioder
      </Heading>
      {perioder.length === 0 ? (
        <BodyShort>Simuleringen har ingen perioder.</BodyShort>
      ) : (
        <Box overflowX="auto" tabIndex={0}>
          {/* Ingen felles tabellhode: hver periode viser kolonnetitlene ved beløpene. */}
          <Table size="small" aria-label="Simulerte perioder med beløp og resultat">
            <Table.Body>
              {perioderMedSisteØverst.map((periode) => (
                <SimuleringsperiodeRad
                  key={`${periode.fom}-${periode.tom}`}
                  periode={periode}
                  beløpskolonner={beløpskolonner}
                />
              ))}
            </Table.Body>
          </Table>
        </Box>
      )}
    </VStack>
  );
}
