import React from "react";
import { BodyShort, Button, Heading, HStack, LocalAlert, Skeleton, VStack } from "@navikt/ds-react";
import { ArrowsCirclepathIcon } from "@navikt/aksel-icons";
import type { OpplysningResponse } from "~/api/generated/types.gen";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { formaterIsoDatoTid } from "~/utils/utils";
import { feilmeldingFra } from "../vilkår/felles/feilmeldingFra";
import { visDato } from "../vilkår/felles/useVilkårSkjema";
import { useMedlemskapBehandlingsgrunnlag } from "./useMedlemskapBehandlingsgrunnlag";
import { MedlemskapOpplysningstabell } from "./MedlemskapOpplysningstabell";

export const MedlemskapBehandlingsgrunnlag: React.FC = () => {
  const { behandlingId } = useBehandlingContext();
  const erLesevisning = useErLesevisning();
  const { medlemskapBehandlingsgrunnlag, error, laster, hentPåNytt, innhentFraPdl } =
    useMedlemskapBehandlingsgrunnlag(behandlingId);
  const [innhentingsfeil, settInnhentingsfeil] = React.useState<string>();

  const innhentFraFolkeregisteret = () => {
    settInnhentingsfeil(undefined);
    innhentFraPdl.kall(
      { path: { behandlingId } },
      {
        onSuccess: hentPåNytt,
        onError: (feil) =>
          settInnhentingsfeil(
            feilmeldingFra(feil, "Kunne ikke hente opplysninger fra Folkeregisteret.")
          ),
      }
    );
  };

  return (
    <VStack gap="space-12">
      <Heading level="3" size="xsmall">
        Opplysninger
      </Heading>

      {laster && <Skeleton variant="rectangle" height={160} />}

      {error !== undefined && (
        <LocalAlert status="error">
          <LocalAlert.Content>
            {feilmeldingFra(error, "Kunne ikke hente opplysningene om medlemskap.")}
          </LocalAlert.Content>
        </LocalAlert>
      )}

      {medlemskapBehandlingsgrunnlag && !medlemskapBehandlingsgrunnlag.hentetTidspunkt && (
        <VStack gap="space-8" align="start">
          <BodyShort size="small">Opplysningene er ikke hentet fra Folkeregisteret.</BodyShort>
          {!erLesevisning && (
            <Button
              type="button"
              size="small"
              variant="secondary"
              icon={<ArrowsCirclepathIcon aria-hidden />}
              loading={innhentFraPdl.laster}
              onClick={innhentFraFolkeregisteret}
            >
              Hent fra Folkeregisteret
            </Button>
          )}
        </VStack>
      )}

      {medlemskapBehandlingsgrunnlag?.hentetTidspunkt && (
        <>
          <HStack gap="space-8" align="center" justify="space-between">
            <BodyShort size="small" textColor="subtle">
              Hentet fra Folkeregisteret{" "}
              {formaterIsoDatoTid(medlemskapBehandlingsgrunnlag.hentetTidspunkt)}
            </BodyShort>
            {!erLesevisning && (
              <Button
                type="button"
                size="xsmall"
                variant="tertiary"
                icon={<ArrowsCirclepathIcon aria-hidden />}
                loading={innhentFraPdl.laster}
                onClick={innhentFraFolkeregisteret}
              >
                Hent på nytt
              </Button>
            )}
          </HStack>
          <MedlemskapOpplysningstabell
            medlemskapBehandlingsgrunnlag={medlemskapBehandlingsgrunnlag}
            visOpplysningsperiode={visOpplysningsperiode}
          />
        </>
      )}

      <div aria-live="polite" aria-atomic="true">
        {innhentingsfeil && (
          <LocalAlert status="error">
            <LocalAlert.Content>{innhentingsfeil}</LocalAlert.Content>
          </LocalAlert>
        )}
      </div>
    </VStack>
  );
};

function visOpplysningsperiode({ fraOgMedDato, tilOgMedDato }: OpplysningResponse): string {
  if (fraOgMedDato && fraOgMedDato === tilOgMedDato) return visDato(fraOgMedDato);
  if (!fraOgMedDato && !tilOgMedDato) return "–";
  return `${fraOgMedDato ? visDato(fraOgMedDato) : ""} – ${tilOgMedDato ? visDato(tilOgMedDato) : ""}`.trim();
}
