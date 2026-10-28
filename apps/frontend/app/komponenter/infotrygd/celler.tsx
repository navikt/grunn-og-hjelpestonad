import React from "react";
import { ClockDashedIcon } from "@navikt/aksel-icons";
import { HStack, InlineMessage, Tag, Tooltip } from "@navikt/ds-react";
import type { InfotrygdEvaluering, InfotrygdKildevedtak } from "~/api/generated/types.gen";
import { formaterBelop } from "~/utils/utils";
import { formaterDato, formaterPeriode } from "./formatering";
import type { Vedtaksrad } from "./vedtaksrader";

type MotstridendeFelt = InfotrygdEvaluering["motstridendeFelt"][number];

export function TomtFelt() {
  return (
    <>
      <span aria-hidden="true">–</span>
      <span className="aksel-sr-only">Ikke registrert</span>
    </>
  );
}

export const visVerdi = (verdi: string | null | undefined): React.ReactNode =>
  verdi?.trim() ? verdi : <TomtFelt />;

export const ingenBryting: React.CSSProperties = { whiteSpace: "nowrap" };

const hentetFraTekst = (kilde: InfotrygdKildevedtak): string =>
  `Hentet fra vedtaket fattet ${formaterDato(kilde.vedtakstidspunkt)}`;

export function FraTidligereVedtak({ kilde }: { kilde: InfotrygdKildevedtak | null }) {
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

export const erMotstridende = (
  evaluering: InfotrygdEvaluering | null,
  felt: MotstridendeFelt
): boolean => evaluering?.motstridendeFelt.includes(felt) ?? false;

// Ikonet vises bare når evalueringen er arvet. Uten kilde står advarselen alene.
export function UlikeVerdier({ kilde }: { kilde?: InfotrygdKildevedtak | null }) {
  const advarsel = (
    <InlineMessage status="warning" size="small">
      Ulike verdier. Les i Infotrygd.
    </InlineMessage>
  );
  if (!kilde) return advarsel;

  return (
    <HStack gap="space-4" align="center" wrap={false}>
      {advarsel}
      <FraTidligereVedtak kilde={kilde} />
    </HStack>
  );
}

export function VerdiMedKilde({
  children,
  kilde,
}: {
  children: React.ReactNode;
  kilde: InfotrygdKildevedtak | null;
}) {
  return (
    <span style={ingenBryting}>
      {children}
      <FraTidligereVedtak kilde={kilde} />
    </span>
  );
}

export function Periode({ rad }: { rad: Vedtaksrad }) {
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

export function Beløp({ rad }: { rad: Vedtaksrad }) {
  switch (rad.beløp.type) {
    case "ETT":
      return <span style={ingenBryting}>{formaterBelop(rad.beløp.beløp)}</span>;
    case "FLERE":
      return <span style={ingenBryting}>Flere, se detaljer</span>;
    case "INGEN":
      return <TomtFelt />;
  }
}
