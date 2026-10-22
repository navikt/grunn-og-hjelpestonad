import React from "react";
import { format, parseISO } from "date-fns";
import { ClockDashedIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Box,
  HGrid,
  HStack,
  Heading,
  InlineMessage,
  Label,
  Table,
  Tag,
  Tooltip,
  VStack,
} from "@navikt/ds-react";
import type { InfotrygdEvaluering, InfotrygdStonaderResponse } from "~/api/generated/types.gen";
import { formaterBelop, formaterIsoDatoTid } from "~/utils/utils";
import { lagVedtaksrader, type Kildevedtak, type Vedtaksrad } from "./vedtaksrader";

type MotstridendeFelt = InfotrygdEvaluering["motstridendeFelt"][number];

const maanedsformat = new Intl.DateTimeFormat("nb-NO", {
  month: "long",
  year: "numeric",
  timeZone: "UTC",
});

const formaterMaaned = (maanedNotert: string): string => {
  const treff = /^(\d{4})-(\d{2})$/.exec(maanedNotert);
  if (!treff) return maanedNotert;

  const aar = Number(treff[1]);
  const maaned = Number(treff[2]);
  if (maaned < 1 || maaned > 12) return maanedNotert;

  try {
    return maanedsformat.format(new Date(Date.UTC(aar, maaned - 1, 1)));
  } catch {
    return maanedNotert;
  }
};

const formaterDato = (dato: string): string => {
  try {
    return format(parseISO(dato), "dd.MM.yyyy");
  } catch {
    return dato;
  }
};

// Streken er skjult for skjermlesere. Teksten «Ikke registrert» leses i stedet.
function TomtFelt() {
  return (
    <>
      <span aria-hidden="true">–</span>
      <span className="aksel-sr-only">Ikke registrert</span>
    </>
  );
}

const visVerdi = (verdi: string | null | undefined): React.ReactNode =>
  verdi?.trim() ? verdi : <TomtFelt />;

function UlikeVerdier() {
  return (
    <InlineMessage status="warning" size="small">
      Ulike verdier. Les i Infotrygd.
    </InlineMessage>
  );
}

const ingenBryting: React.CSSProperties = { whiteSpace: "nowrap" };

const hentetFraTekst = (kilde: Kildevedtak): string =>
  `Hentet fra vedtaket fattet ${formaterDato(kilde.vedtakstidspunkt)}`;

// Ikon i stedet for tagg, så cellene ikke brytes. Tooltip setter aria-label på spennet.
function FraTidligereVedtak({ kilde }: { kilde: Kildevedtak | null }) {
  if (!kilde) return null;
  return (
    <>
      {" "}
      <Tooltip content={hentetFraTekst(kilde)}>
        <span role="img" tabIndex={0} style={{ display: "inline-flex", verticalAlign: "middle" }}>
          <ClockDashedIcon aria-hidden fontSize="1.25rem" />
        </span>
      </Tooltip>
    </>
  );
}

// Med verdi og merke i samme spenn havner ikke ikonet på egen linje.
function VerdiMedKilde({
  children,
  kilde,
}: {
  children: React.ReactNode;
  kilde: Kildevedtak | null;
}) {
  return (
    <span style={ingenBryting}>
      {children}
      <FraTidligereVedtak kilde={kilde} />
    </span>
  );
}

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

const formaterPeriode = (fom: string, tom: string | null | undefined): string =>
  tom ? `${formaterDato(fom)} – ${formaterDato(tom)}` : `${formaterDato(fom)} –`;

function Periode({ rad }: { rad: Vedtaksrad }) {
  const { vedtak } = rad;
  return (
    <HStack gap="space-4" align="center" wrap={false}>
      <span style={ingenBryting}>{formaterPeriode(vedtak.datoInnvFom, vedtak.datoInnvTom)}</span>
      {rad.løpende && (
        <Tag variant="moderate" size="xsmall" data-color="info">
          Løpende
        </Tag>
      )}
      {rad.opphørt && (
        <Tag variant="moderate" size="xsmall" data-color="neutral">
          {`Opphørt ${formaterDato(rad.opphørt)}`}
        </Tag>
      )}
      {rad.ikkeIverksatt && (
        <Tag variant="moderate" size="xsmall" data-color="warning">
          Ikke iverksatt
        </Tag>
      )}
    </HStack>
  );
}

function Beløp({ rad }: { rad: Vedtaksrad }) {
  switch (rad.beløp.type) {
    case "ETT":
      return <span style={ingenBryting}>{formaterBelop(rad.beløp.beløp)}</span>;
    case "FLERE":
      return <span style={ingenBryting}>Flere, se detaljer</span>;
    case "INGEN":
      return <TomtFelt />;
  }
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
  const fattet = (kilde: Kildevedtak) =>
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
              {[
                "Type",
                "Fra og med",
                "Til og med",
                "Beløp",
                "Type sats",
                "Type utbetaling",
                "Registrert",
              ].map((tittel) => (
                <Table.HeaderCell key={tittel} scope="col" style={ingenBryting}>
                  {tittel}
                </Table.HeaderCell>
              ))}
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {rad.delytelser.map((d) => (
              <Table.Row key={`${d.typeDelytelse}-${d.tidspunktReg}`}>
                <Table.DataCell>{d.typeDelytelse}</Table.DataCell>
                <Table.DataCell>{formaterDato(d.fom)}</Table.DataCell>
                <Table.DataCell>{d.tom ? formaterDato(d.tom) : <TomtFelt />}</Table.DataCell>
                <Table.DataCell style={ingenBryting}>{formaterBelop(d.belop)}</Table.DataCell>
                <Table.DataCell>{d.typeSats}</Table.DataCell>
                <Table.DataCell>{d.typeUtbetaling}</Table.DataCell>
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

function Detaljer({
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
                <Diagnose kode={evaluering.diagnose1} diagnosetekster={diagnosetekster} />
              </Felt>
              <Felt tittel="Diagnose 2">
                <Diagnose kode={evaluering.diagnose2} diagnosetekster={diagnosetekster} />
              </Felt>
              <Felt tittel="Sats i evalueringen">{visVerdi(evaluering.sats)}</Felt>
              <Felt tittel="Måned notert i Infotrygd">
                {formaterMaaned(evaluering.månedNotertIInfotrygd)}
              </Felt>
            </>
          )}
          <Felt tittel="Endringskoder">
            {vedtak.endringer.length > 0 ? vedtak.endringer.join(", ") : <TomtFelt />}
          </Felt>
          <Felt tittel="Innvilget">
            {formaterDato(vedtak.datoInnvFom)} –{" "}
            {vedtak.datoInnvTom ? formaterDato(vedtak.datoInnvTom) : <TomtFelt />}
          </Felt>
          <Felt tittel="Vedtakstidspunkt">{formaterIsoDatoTid(vedtak.tidspunktReg)}</Felt>
          <Felt tittel="Type sak og resultat">
            {visVerdi(vedtak.typeSak)} / {visVerdi(vedtak.kodeResultat)}
          </Felt>
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

function Radvisning({
  rad,
  diagnosetekster,
}: {
  rad: Vedtaksrad;
  diagnosetekster: Record<string, string>;
}) {
  const { vedtak, evaluering } = rad;
  const motstridende = (felt: MotstridendeFelt) => evaluering?.motstridendeFelt.includes(felt);
  const nyesteGh = rad.gh[0];
  const sats = nyesteGh?.satsGs;
  const ghKilde = rad.ghFraVedtak;
  const evalueringskilde = rad.evalueringFraVedtak;
  const virkningstidspunkt = evaluering?.virkningstidspunkt;
  const hjemmelbokstav = evaluering?.hjemmelbokstav?.trim();

  return (
    <Table.ExpandableRow
      content={<Detaljer rad={rad} diagnosetekster={diagnosetekster} />}
      togglePlacement="left"
    >
      <Table.DataCell>
        <Periode rad={rad} />
      </Table.DataCell>
      <Table.DataCell>
        <Beløp rad={rad} />
      </Table.DataCell>
      <Table.DataCell>
        {sats != null ? <VerdiMedKilde kilde={ghKilde}>{sats}</VerdiMedKilde> : <TomtFelt />}
      </Table.DataCell>
      <Table.DataCell>
        {nyesteGh ? (
          <VerdiMedKilde kilde={ghKilde}>{`${nyesteGh.trygdetid} år`}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>
        {motstridende("HJEMMELBOKSTAV") ? (
          <UlikeVerdier />
        ) : hjemmelbokstav ? (
          <VerdiMedKilde kilde={evalueringskilde}>{hjemmelbokstav}</VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>
        {motstridende("VIRKNINGSTIDSPUNKT") ? (
          <UlikeVerdier />
        ) : virkningstidspunkt ? (
          <VerdiMedKilde kilde={evalueringskilde}>
            {formaterMaaned(virkningstidspunkt)}
          </VerdiMedKilde>
        ) : (
          <TomtFelt />
        )}
      </Table.DataCell>
      <Table.DataCell>{formaterDato(vedtak.datoMottattSak)}</Table.DataCell>
      <Table.DataCell>{formaterDato(vedtak.tidspunktReg)}</Table.DataCell>
      <Table.DataCell>{vedtak.brukerid}</Table.DataCell>
    </Table.ExpandableRow>
  );
}

const KOLONNER = [
  "Periode",
  "Beløp",
  "Sats",
  "Trygdetid",
  "Bokstav i § 6-3",
  "Virkningstidspunkt",
  "Søknad mottatt",
  "Vedtatt",
  "Saksbehandler",
];

export function InfotrygdVedtak({ data }: { data: InfotrygdStonaderResponse }) {
  const rader = lagVedtaksrader(data.stonader);
  const overskriftId = "infotrygd-grunnstonad";

  return (
    <VStack gap="space-8">
      <Heading level="2" size="small" id={overskriftId}>
        Grunnstønad
      </Heading>
      {rader.length === 0 ? (
        <BodyShort>Ingen vedtak på grunnstønad i Infotrygd</BodyShort>
      ) : (
        // tabIndex gjør den horisontalt scrollbare boksen tilgjengelig med tastatur.
        <Box overflowX="auto" tabIndex={0}>
          <Table size="small" aria-labelledby={overskriftId}>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                {KOLONNER.map((tittel) => (
                  <Table.HeaderCell key={tittel} scope="col" style={ingenBryting}>
                    {tittel}
                  </Table.HeaderCell>
                ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {rader.map((rad) => (
                <Radvisning
                  key={rad.vedtak.vedtakId}
                  rad={rad}
                  diagnosetekster={data.diagnosetekster}
                />
              ))}
            </Table.Body>
          </Table>
        </Box>
      )}
    </VStack>
  );
}
