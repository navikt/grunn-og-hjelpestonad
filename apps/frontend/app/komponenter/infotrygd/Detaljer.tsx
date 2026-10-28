import React from "react";
import { BodyShort, Box, HGrid, Label, Table, VStack } from "@navikt/ds-react";
import type { InfotrygdKildevedtak } from "~/api/generated/types.gen";
import { formaterBelop, formaterIsoDatoTid } from "~/utils/utils";
import { formaterDato, formaterMaaned } from "./formatering";
import { formaterEndringskode } from "./endringskoder";
import { formaterHjemmelbokstav, formaterResultat, formaterSakstype } from "./vedtakskoder";
import { erMotstridende, ingenBryting, TomtFelt, UlikeVerdier, visVerdi } from "./celler";
import type { Vedtaksrad } from "./vedtaksrader";

function Diagnose({
  kode,
  diagnosetekster,
}: {
  kode: string | null | undefined;
  diagnosetekster: Record<string, string>;
}) {
  const renset = kode?.trim();
  if (!renset) return <TomtFelt />;

  const icd10Tekst = diagnosetekster[renset]?.trim();
  return (
    <>
      {renset}
      <BodyShort size="small" textColor="subtle">
        {icd10Tekst ? icd10Tekst : "Ingen tekst for ICD-10-koden"}
      </BodyShort>
    </>
  );
}

function Felt({ tittel, children }: { tittel: string; children: React.ReactNode }) {
  return (
    <VStack gap="space-2">
      <Label size="small">{tittel}</Label>
      <BodyShort size="small" as="div">
        {children}
      </BodyShort>
    </VStack>
  );
}

// Én samlet setning om hvor arvede verdier kommer fra, i stedet for én per seksjon.
function Kildetekst({ rad }: { rad: Vedtaksrad }) {
  const evaluering = rad.evalueringFraVedtak;
  const trygdetid = rad.ghFraVedtak;
  const fattet = (kilde: InfotrygdKildevedtak) =>
    `er hentet fra vedtaket fattet ${formaterDato(kilde.vedtakstidspunkt)}.`;

  const setninger =
    evaluering && trygdetid && evaluering.vedtakId === trygdetid.vedtakId
      ? [`Sats, trygdetid og evaluering ${fattet(evaluering)}`]
      : [
          evaluering ? `Evalueringen ${fattet(evaluering)}` : null,
          trygdetid ? `Trygdetid og sats ${fattet(trygdetid)}` : null,
        ].filter((setning) => setning !== null);
  if (setninger.length === 0) return null;

  return (
    <BodyShort size="small" textColor="subtle">
      {setninger.join(" ")}
    </BodyShort>
  );
}

function Delytelser({ rad }: { rad: Vedtaksrad }) {
  if (rad.delytelser.length === 0) {
    return <BodyShort size="small">Ingen delytelser registrert på dette vedtaket.</BodyShort>;
  }
  return (
    <VStack gap="space-8">
      <Label size="small" as="p">
        Delytelser
      </Label>
      <Box overflowX="auto" tabIndex={0}>
        <Table size="small" aria-label="Delytelser">
          <Table.Header>
            <Table.Row>
              {["Fra og med", "Til og med", "Beløp", "Registrert"].map((tittel) => (
                <Table.HeaderCell key={tittel} scope="col" style={ingenBryting}>
                  {tittel}
                </Table.HeaderCell>
              ))}
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {rad.delytelser.map((d) => (
              <Table.Row key={`${d.typeDelytelse}-${d.tidspunktReg}`}>
                <Table.DataCell>{formaterDato(d.fom)}</Table.DataCell>
                <Table.DataCell>{d.tom ? formaterDato(d.tom) : <TomtFelt />}</Table.DataCell>
                <Table.DataCell style={ingenBryting}>{formaterBelop(d.belop)}</Table.DataCell>
                <Table.DataCell>{formaterIsoDatoTid(d.tidspunktReg)}</Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  );
}

function TrygdetidOgSats({ rad }: { rad: Vedtaksrad }) {
  if (rad.gh.length === 0) return null;
  return (
    <VStack gap="space-8">
      <Label size="small" as="p">
        Trygdetid og sats
      </Label>
      <Box overflowX="auto" tabIndex={0}>
        <Table size="small" aria-label="Trygdetid og sats">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Registrert</Table.HeaderCell>
              <Table.HeaderCell scope="col">Trygdetid</Table.HeaderCell>
              <Table.HeaderCell scope="col">Sats</Table.HeaderCell>
              <Table.HeaderCell scope="col">Registrert av</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {rad.gh.map((g) => (
              <Table.Row key={`${g.tidspunktReg}-${g.brukerid}`}>
                <Table.DataCell>{formaterDato(g.tidspunktReg)}</Table.DataCell>
                <Table.DataCell>{g.trygdetid} år</Table.DataCell>
                <Table.DataCell>{g.satsGs != null ? g.satsGs : <TomtFelt />}</Table.DataCell>
                <Table.DataCell>{g.brukerid}</Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  );
}

export function Detaljer({
  rad,
  diagnosetekster,
}: {
  rad: Vedtaksrad;
  diagnosetekster: Record<string, string>;
}) {
  const { vedtak, stønad, evaluering } = rad;

  return (
    <Box background="neutral-soft" padding="space-16" borderRadius="4">
      <VStack gap="space-16">
        {!evaluering && (
          <BodyShort size="small">Ingen evaluering registrert på dette vedtaket.</BodyShort>
        )}

        {/* Samme grid for alle feltene, så kolonnene står på linje. */}
        <HGrid columns={{ xs: 1, md: 4 }} gap="space-16">
          {evaluering && (
            <>
              <Felt tittel="Diagnose 1">
                {erMotstridende(evaluering, "DIAGNOSE_1") ? (
                  <UlikeVerdier />
                ) : (
                  <Diagnose kode={evaluering.diagnose1} diagnosetekster={diagnosetekster} />
                )}
              </Felt>
              <Felt tittel="Diagnose 2">
                {erMotstridende(evaluering, "DIAGNOSE_2") ? (
                  <UlikeVerdier />
                ) : (
                  <Diagnose kode={evaluering.diagnose2} diagnosetekster={diagnosetekster} />
                )}
              </Felt>
              <Felt tittel="Ekstrautgift">
                {erMotstridende(evaluering, "HJEMMELBOKSTAV") ? (
                  <UlikeVerdier />
                ) : evaluering.hjemmelbokstav?.trim() ? (
                  formaterHjemmelbokstav(evaluering.hjemmelbokstav)
                ) : (
                  <TomtFelt />
                )}
              </Felt>
              <Felt tittel="Sats i evalueringen">
                {erMotstridende(evaluering, "SATS") ? <UlikeVerdier /> : visVerdi(evaluering.sats)}
              </Felt>
              <Felt tittel="Måned notert i Infotrygd">
                {formaterMaaned(evaluering.månedNotertIInfotrygd)}
              </Felt>
            </>
          )}
          <Felt tittel="Endringskoder">
            {vedtak.endringer.length > 0 ? (
              vedtak.endringer.map((kode) => <div key={kode}>{formaterEndringskode(kode)}</div>)
            ) : (
              <TomtFelt />
            )}
          </Felt>
          <Felt tittel="Innvilget">
            {formaterDato(vedtak.datoInnvFom)} –{" "}
            {vedtak.datoInnvTom ? formaterDato(vedtak.datoInnvTom) : <TomtFelt />}
          </Felt>
          <Felt tittel="Vedtakstidspunkt">{formaterIsoDatoTid(vedtak.tidspunktReg)}</Felt>
          <Felt tittel="Sakstype">{visVerdi(formaterSakstype(vedtak.typeSak))}</Felt>
          <Felt tittel="Resultat">{visVerdi(formaterResultat(vedtak.kodeResultat))}</Felt>
          <Felt tittel="Stønaden startet">{formaterDato(stønad.datoStart)}</Felt>
          <Felt tittel="Stønaden opphørte">
            {stønad.datoOpphor ? formaterDato(stønad.datoOpphor) : <TomtFelt />}
          </Felt>
        </HGrid>

        <Delytelser rad={rad} />
        <TrygdetidOgSats rad={rad} />
        <Kildetekst rad={rad} />
      </VStack>
    </Box>
  );
}
