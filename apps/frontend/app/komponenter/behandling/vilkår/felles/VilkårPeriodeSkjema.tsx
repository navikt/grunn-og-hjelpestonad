import React, { useEffect, useRef } from "react";
import {
  Button,
  DatePicker,
  ErrorSummary,
  Fieldset,
  HStack,
  LocalAlert,
  Radio,
  RadioGroup,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { Vurdering } from "~/types/vilkår";
import type { FellesFelt, VilkårSkjema } from "./useVilkårSkjema";

/** Typespesifikke felter som skal med i feiloppsummeringen, i den rekkefølgen de vises. */
export interface EkstraFelt<Felt extends string = never> {
  felt: Felt;
  id: string;
}

interface VilkårPeriodeSkjemaProps<Felt extends string> {
  idPrefiks: string;
  spørsmål: string;
  skjema: VilkårPeriodeSkjemaState<Felt>;
  lagrer: boolean;
  onLagre: () => void;
  onVurderingEndret?: (vurdering: Vurdering) => void;
  ekstraFelter?: EkstraFelt<Felt>[];
  children?: React.ReactNode;
}

type VilkårPeriodeSkjemaState<Felt extends string> = Omit<
  VilkårSkjema<Felt>,
  "åpneNyPeriode" | "åpneRedigeringAvPeriode"
>;

export function VilkårPeriodeSkjema<Felt extends string = never>({
  idPrefiks,
  spørsmål,
  skjema,
  lagrer,
  onLagre,
  onVurderingEndret,
  ekstraFelter = [],
  children,
}: VilkårPeriodeSkjemaProps<Felt>) {
  const feiloppsummeringRef = useRef<HTMLDivElement>(null);
  const fokusTeller = skjema.fokusFeiloppsummering;

  useEffect(() => {
    if (fokusTeller > 0) {
      feiloppsummeringRef.current?.focus();
    }
  }, [fokusTeller]);

  const feltrekkefølge: EkstraFelt<FellesFelt | Felt>[] = [
    { felt: "vurdering", id: `${idPrefiks}-vurdering` },
    ...ekstraFelter,
    { felt: "fraOgMed", id: `${idPrefiks}-fraOgMed` },
    { felt: "tilOgMed", id: `${idPrefiks}-tilOgMed` },
    { felt: "begrunnelse", id: `${idPrefiks}-begrunnelse` },
  ];

  const feilIRekkefølge = feltrekkefølge
    .map(({ felt, id }) => ({ id, tekst: skjema.feil[felt] }))
    .filter((feil): feil is { id: string; tekst: string } => Boolean(feil.tekst));

  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        skjema.forsøkLagre(onLagre);
      }}
    >
      <VStack gap="space-24">
        <RadioGroup
          legend={spørsmål}
          value={skjema.vurdering}
          onChange={(verdi: Vurdering) => {
            skjema.settVurdering(verdi);
            skjema.fjernFeil("vurdering");
            onVurderingEndret?.(verdi);
          }}
          error={skjema.feil.vurdering}
        >
          <Radio id={`${idPrefiks}-vurdering`} value={Vurdering.JA}>
            Ja
          </Radio>
          <Radio value={Vurdering.NEI}>Nei</Radio>
        </RadioGroup>

        {children}

        <DatePicker {...skjema.datepickerProps}>
          <Fieldset
            legend="Periode"
            description="La feltene stå tomme hvis vurderingen gjelder hele behandlingsperioden."
            errorPropagation={false}
          >
            <HStack gap="space-16" wrap>
              <DatePicker.Input
                {...skjema.fromInputProps}
                id={`${idPrefiks}-fraOgMed`}
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
                id={`${idPrefiks}-tilOgMed`}
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

        <Textarea
          id={`${idPrefiks}-begrunnelse`}
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
}
