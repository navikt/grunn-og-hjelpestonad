import React from "react";
import {
  Box,
  Button,
  Checkbox,
  CheckboxGroup,
  Fieldset,
  LocalAlert,
  Select,
  VStack,
} from "@navikt/ds-react";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import {
  formatertKode,
  visningAvDiagnosekode,
} from "~/komponenter/behandling/vilkår/diagnose/diagnosekode";
import {
  hjemmelbokstavTekst,
  type DiagnosekodeResponse,
  type Hjemmelbokstav,
} from "~/types/vilkår";
import type { EkstrautgifterSkjema } from "./useEkstrautgifterVilkårSkjema";

export type EkstrautgiftFelterSkjema = Pick<
  EkstrautgifterSkjema,
  | "ekstrautgifter"
  | "radfeil"
  | "listefeil"
  | "endreHjemmelbokstav"
  | "endreDiagnoser"
  | "fjernEkstrautgift"
  | "leggTilEkstrautgift"
  | "valgbareHjemmelbokstaver"
  | "kanLeggeTilEkstrautgift"
>;

interface EkstrautgiftFelterProps {
  skjema: EkstrautgiftFelterSkjema;
  diagnoserVurdertTilJa: DiagnosekodeResponse[];
}

export const EkstrautgiftFelter: React.FC<EkstrautgiftFelterProps> = ({
  skjema,
  diagnoserVurdertTilJa,
}) => {
  // Koder som raden allerede har, men som ikke er vurdert til ja lenger, vises slik at de kan fjernes.
  const diagnosevalg = (diagnosekoder: string[]) => {
    const kjente = new Set(diagnoserVurdertTilJa.map((diagnose) => diagnose.kode));
    const ukjente = diagnosekoder.filter((kode) => !kjente.has(kode));

    return [
      ...diagnoserVurdertTilJa.map((diagnose) => ({
        kode: diagnose.kode,
        tekst: visningAvDiagnosekode(diagnose),
      })),
      ...ukjente.map((kode) => ({ kode, tekst: formatertKode(kode) })),
    ];
  };

  return (
    <Fieldset
      id="ekstrautgifter-liste"
      legend="Ekstrautgifter"
      description="Velg hjemmelbokstav i § 6-3 første ledd og diagnosene som gir ekstrautgiften."
      error={skjema.listefeil}
      errorPropagation={false}
      tabIndex={-1}
    >
      <VStack gap="space-16">
        {diagnoserVurdertTilJa.length === 0 && (
          <LocalAlert status="warning">
            <LocalAlert.Content>
              Ingen diagnoser er vurdert til oppfylt. Vurder diagnose i steg 1 før du legger til
              ekstrautgifter.
            </LocalAlert.Content>
          </LocalAlert>
        )}
        {/* Lagrede ekstrautgifter vises også uten diagnoser vurdert til ja, slik at de kan fjernes. */}
        {skjema.ekstrautgifter.map((rad, indeks) => {
          const valg = diagnosevalg(rad.diagnosekoder);

          return (
            <Box
              key={indeks}
              borderColor="neutral"
              borderWidth="1"
              borderRadius="12"
              padding="space-16"
            >
              <Fieldset legend={`Ekstrautgift ${indeks + 1}`}>
                <VStack gap="space-16">
                  <Select
                    id={`ekstrautgifter-${indeks}-hjemmelbokstav`}
                    label="Hjemmelbokstav"
                    value={rad.hjemmelbokstav}
                    error={skjema.radfeil[indeks]?.hjemmelbokstav}
                    onChange={(event) =>
                      skjema.endreHjemmelbokstav(indeks, event.target.value as Hjemmelbokstav | "")
                    }
                  >
                    <option value="">Velg hjemmelbokstav</option>
                    {skjema.valgbareHjemmelbokstaver(indeks).map((bokstav) => (
                      <option key={bokstav} value={bokstav}>
                        {hjemmelbokstavTekst[bokstav]}
                      </option>
                    ))}
                  </Select>

                  <CheckboxGroup
                    legend="Diagnoser"
                    id={valg.length === 0 ? `ekstrautgifter-${indeks}-diagnoser` : undefined}
                    tabIndex={valg.length === 0 ? -1 : undefined}
                    value={rad.diagnosekoder}
                    error={skjema.radfeil[indeks]?.diagnoser}
                    onChange={(koder: string[]) => skjema.endreDiagnoser(indeks, koder)}
                  >
                    {valg.map(({ kode, tekst }, kodeIndeks) => (
                      <Checkbox
                        key={kode}
                        id={kodeIndeks === 0 ? `ekstrautgifter-${indeks}-diagnoser` : undefined}
                        aria-invalid={Boolean(skjema.radfeil[indeks]?.diagnoser) || undefined}
                        value={kode}
                      >
                        {tekst}
                      </Checkbox>
                    ))}
                  </CheckboxGroup>

                  <div>
                    <Button
                      type="button"
                      variant="tertiary"
                      size="small"
                      icon={<TrashIcon aria-hidden />}
                      onClick={() => skjema.fjernEkstrautgift(indeks)}
                    >
                      Fjern ekstrautgift
                    </Button>
                  </div>
                </VStack>
              </Fieldset>
            </Box>
          );
        })}

        {skjema.kanLeggeTilEkstrautgift && diagnoserVurdertTilJa.length > 0 && (
          <div>
            <Button
              type="button"
              variant="secondary"
              size="small"
              icon={<PlusIcon aria-hidden />}
              onClick={skjema.leggTilEkstrautgift}
            >
              Legg til ekstrautgift
            </Button>
          </div>
        )}
      </VStack>
    </Fieldset>
  );
};
