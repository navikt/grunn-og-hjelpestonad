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

interface EkstrautgiftFelterProps {
  skjema: EkstrautgifterSkjema;
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
      error={skjema.feil.ekstrautgifter}
      errorPropagation={false}
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
        {skjema.ekstrautgifter.map((rad, indeks) => (
          <Box
            key={indeks}
            borderColor="neutral"
            borderWidth="1"
            borderRadius="12"
            padding="space-16"
          >
            <VStack gap="space-16">
              <Select
                id={`ekstrautgifter-${indeks}-hjemmelbokstav`}
                label="Hjemmelbokstav"
                value={rad.hjemmelbokstav}
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
                value={rad.diagnosekoder}
                onChange={(koder: string[]) => skjema.endreDiagnoser(indeks, koder)}
              >
                {diagnosevalg(rad.diagnosekoder).map(({ kode, tekst }) => (
                  <Checkbox key={kode} value={kode}>
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
          </Box>
        ))}

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
