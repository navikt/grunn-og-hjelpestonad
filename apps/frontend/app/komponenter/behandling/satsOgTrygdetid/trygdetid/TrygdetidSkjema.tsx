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
  TextField,
  VStack,
} from "@navikt/ds-react";
import { MAKS_TELLER, type TrygdetidSkjemaState } from "./useTrygdetidSkjema";

const ID_PREFIKS = "trygdetid";
const FRA_OG_MED_ID = `${ID_PREFIKS}-fraOgMed`;
const TIL_OG_MED_ID = `${ID_PREFIKS}-tilOgMed`;
const KILDE_ID = `${ID_PREFIKS}-kilde`;
const TELLER_ID = `${ID_PREFIKS}-teller`;
const BEGRUNNELSE_ID = `${ID_PREFIKS}-begrunnelse`;

interface TrygdetidSkjemaProps {
  skjema: TrygdetidSkjemaState;
  lagrer: boolean;
  onLagre: () => void;
}

export const TrygdetidSkjema: React.FC<TrygdetidSkjemaProps> = ({ skjema, lagrer, onLagre }) => {
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);
  const fokusTeller = skjema.fokusFeiloppsummering;

  useEffect(() => {
    if (fokusTeller > 0) {
      feiloppsummeringRef.current?.focus();
    }
  }, [fokusTeller]);

  const feilIRekkefølge = [
    { id: FRA_OG_MED_ID, tekst: skjema.skjemafeil.fraOgMed },
    { id: TIL_OG_MED_ID, tekst: skjema.skjemafeil.tilOgMed },
    { id: KILDE_ID, tekst: skjema.skjemafeil.kilde },
    { id: TELLER_ID, tekst: skjema.skjemafeil.teller },
  ].filter((feil): feil is { id: string; tekst: string } => Boolean(feil.tekst));

  const kildenErIngen = skjema.kilde === "INGEN";

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
          <Fieldset
            legend="Periode"
            description="La feltene stå tomme hvis trygdetiden gjelder hele behandlingsperioden."
            errorPropagation={false}
          >
            <HStack gap="space-16" wrap>
              <DatePicker.Input
                {...skjema.fromInputProps}
                id={FRA_OG_MED_ID}
                label="Fra og med"
                error={skjema.skjemafeil.fraOgMed}
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
                error={skjema.skjemafeil.tilOgMed}
                onBlur={(event) => {
                  skjema.toInputProps.onBlur?.(event);
                  skjema.validerFelt("tilOgMed");
                }}
              />
            </HStack>
          </Fieldset>
        </DatePicker>

        <Select
          id={KILDE_ID}
          label="Kilde"
          value={skjema.kilde}
          onChange={(event) => skjema.velgKilde(event.target.value)}
          error={skjema.skjemafeil.kilde}
        >
          <option value="">Velg kilde</option>
          <option value="ALDERSPENSJON">Alderspensjon</option>
          <option value="UFØRETRYGD">Uføretrygd</option>
          <option value="INGEN">Ingen</option>
          {/* Vises bare når raden allerede er fra migrering. Den kan ikke velges på nye rader. */}
          {skjema.erMigrertPeriode && <option value="MIGRERING">Fra migrering</option>}
        </Select>

        <TextField
          id={TELLER_ID}
          label="Teller"
          description={
            kildenErIngen
              ? `Kilden «Ingen» gir alltid ${MAKS_TELLER}, så telleren er låst.`
              : `Heltall fra 0 til ${MAKS_TELLER}.`
          }
          inputMode="numeric"
          readOnly={kildenErIngen}
          value={skjema.teller}
          onChange={(event) => skjema.endreTeller(event.target.value)}
          error={skjema.skjemafeil.teller}
        />

        <Textarea
          id={BEGRUNNELSE_ID}
          label="Begrunnelse"
          description="Valgfritt. Forklar hvor trygdetiden kommer fra."
          maxLength={2000}
          value={skjema.begrunnelse}
          onChange={(event) => {
            skjema.settBegrunnelse(event.target.value);
          }}
        />

        {/*
          Live-regionen må ligge i DOM-en før meldingen kommer, ellers leser ikke skjermlesere
          den opp. Derfor er wrapperen alltid rendret, mens innholdet byttes ut.
        */}
        <div aria-live="assertive" aria-atomic="true">
          {skjema.backendfeil && (
            <LocalAlert status="error">
              <LocalAlert.Content>
                {skjema.backendfeil} Endringene er ikke lagret. Prøv igjen.
              </LocalAlert.Content>
            </LocalAlert>
          )}
        </div>

        {skjema.harForsøktLagre && feilIRekkefølge.length > 0 && (
          <ErrorSummary
            ref={feiloppsummeringRef}
            heading="Du må rette disse feilene før du kan lagre trygdetiden"
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
            {skjema.redigererId ? "Lagre endringer" : "Lagre trygdetid"}
          </Button>
          <Button type="button" variant="secondary" onClick={skjema.lukk} disabled={lagrer}>
            Avbryt
          </Button>
        </HStack>
      </VStack>
    </form>
  );
};
