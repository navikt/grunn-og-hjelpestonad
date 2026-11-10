import React, { useEffect, useRef } from "react";
import {
  Button,
  DatePicker,
  ErrorSummary,
  Fieldset,
  HStack,
  LocalAlert,
  Select,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { satsTekst, Vurdering, type DiagnosekodeResponse } from "~/types/vilkår";
import {
  UNDER_SATS_1,
  type EkstrautgifterSkjema as EkstrautgifterSkjemaState,
  type SatsValg,
} from "./useEkstrautgifterVilkårSkjema";
import { EkstrautgiftFelter } from "./EkstrautgiftFelter";

const ID_PREFIKS = "ekstrautgifter";
const FRA_OG_MED_ID = `${ID_PREFIKS}-fraOgMed`;
const TIL_OG_MED_ID = `${ID_PREFIKS}-tilOgMed`;
const LISTE_ID = `${ID_PREFIKS}-liste`;
const SATS_ID = `${ID_PREFIKS}-sats`;
const BEGRUNNELSE_ID = `${ID_PREFIKS}-begrunnelse`;

interface EkstrautgifterSkjemaProps {
  skjema: EkstrautgifterSkjemaState;
  diagnoserVurdertTilJa: DiagnosekodeResponse[];
  lagrer: boolean;
  onLagre: () => void;
}

export const EkstrautgifterSkjema: React.FC<EkstrautgifterSkjemaProps> = ({
  skjema,
  diagnoserVurdertTilJa,
  lagrer,
  onLagre,
}) => {
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);
  const fokusTeller = skjema.fokusFeiloppsummering;

  useEffect(() => {
    if (fokusTeller > 0) {
      feiloppsummeringRef.current?.focus();
    }
  }, [fokusTeller]);

  const periodeBeskrivelse =
    skjema.vurdering === Vurdering.JA
      ? "Fra og med-datoen er påkrevd. La til og med stå tom hvis perioden er løpende."
      : "La feltene stå tomme hvis vurderingen gjelder hele behandlingsperioden.";

  // Vurdering og sats deler id. Bare én melding vises for dem, og vurdering vinner.
  const feilIRekkefølge = [
    { id: FRA_OG_MED_ID, tekst: skjema.feil.fraOgMed },
    { id: TIL_OG_MED_ID, tekst: skjema.feil.tilOgMed },
    { id: LISTE_ID, tekst: skjema.feil.ekstrautgifter },
    { id: SATS_ID, tekst: skjema.feil.vurdering || skjema.feil.sats },
    { id: BEGRUNNELSE_ID, tekst: skjema.feil.begrunnelse },
  ].filter((feil): feil is { id: string; tekst: string } => Boolean(feil.tekst));

  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        skjema.forsøkLagre(onLagre);
      }}
    >
      <VStack gap="space-24">
        <DatePicker {...skjema.datepickerProps}>
          <Fieldset legend="Periode" description={periodeBeskrivelse} errorPropagation={false}>
            <HStack gap="space-16" wrap>
              <DatePicker.Input
                {...skjema.fromInputProps}
                id={FRA_OG_MED_ID}
                label="Fra og med"
                error={skjema.feil.fraOgMed}
                onChange={(event) => {
                  skjema.fromInputProps.onChange?.(event);
                }}
                onBlur={(event) => {
                  skjema.fromInputProps.onBlur?.(event);
                  skjema.validerFelt("fraOgMed");
                  skjema.validerFelt("tilOgMed");
                }}
              />
              <DatePicker.Input
                {...skjema.toInputProps}
                id={TIL_OG_MED_ID}
                label="Til og med"
                error={skjema.feil.tilOgMed}
                onChange={(event) => {
                  skjema.toInputProps.onChange?.(event);
                }}
                onBlur={(event) => {
                  skjema.toInputProps.onBlur?.(event);
                  skjema.validerFelt("tilOgMed");
                }}
              />
            </HStack>
          </Fieldset>
        </DatePicker>

        <EkstrautgiftFelter skjema={skjema} diagnoserVurdertTilJa={diagnoserVurdertTilJa} />

        <Select
          id={SATS_ID}
          label="Sats for grunnstønad"
          value={skjema.satsValg}
          onChange={(event) => skjema.velgSatsValg(event.target.value as SatsValg)}
          error={skjema.feil.vurdering ?? skjema.feil.sats}
        >
          <option value="">Velg sats</option>
          <option value={UNDER_SATS_1}>Ekstrautgifter under sats 1</option>
          {Object.entries(satsTekst).map(([verdi, tekst]) => (
            <option key={verdi} value={verdi}>
              {tekst}
            </option>
          ))}
        </Select>

        <Textarea
          id={BEGRUNNELSE_ID}
          label="Begrunnelse"
          description="Valgfritt. Forklar hva vurderingen bygger på."
          maxLength={2000}
          value={skjema.begrunnelse}
          onChange={(event) => {
            skjema.settBegrunnelse(event.target.value);
          }}
          onBlur={() => skjema.validerFelt("begrunnelse")}
          error={skjema.feil.begrunnelse}
        />

        {/*
          Live-regionen må ligge i DOM-en før meldingen kommer, ellers leser ikke skjermlesere
          den opp. Derfor er wrapperen alltid rendret, mens innholdet byttes ut.
        */}
        <div aria-live="assertive" aria-atomic="true">
          {skjema.lagringsfeil && (
            <LocalAlert status="error">
              <LocalAlert.Content>
                {skjema.lagringsfeil} Endringene er ikke lagret. Prøv igjen.
              </LocalAlert.Content>
            </LocalAlert>
          )}
        </div>

        {skjema.harForsøktLagre && feilIRekkefølge.length > 0 && (
          <ErrorSummary
            ref={feiloppsummeringRef}
            heading="Du må rette disse feilene før du kan lagre perioden:"
          >
            {feilIRekkefølge.map((feil) => (
              <ErrorSummary.Item key={feil.id} href={`#${feil.id}`}>
                {feil.tekst}
              </ErrorSummary.Item>
            ))}
          </ErrorSummary>
        )}

        <HStack gap="space-8">
          <Button type="submit" loading={lagrer}>
            {skjema.redigererId ? "Lagre endringer" : "Lagre periode"}
          </Button>
          <Button type="button" variant="secondary" onClick={skjema.lukk}>
            Avbryt
          </Button>
        </HStack>
      </VStack>
    </form>
  );
};
