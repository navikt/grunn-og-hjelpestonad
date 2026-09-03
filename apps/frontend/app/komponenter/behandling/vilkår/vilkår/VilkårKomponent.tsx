import {
  BodyLong,
  Box,
  Button,
  Heading,
  HStack,
  Radio,
  RadioGroup,
  Stack,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import React from "react";
import styles from "./VilkårKomponent.module.css";
import {
  CheckmarkCircleFillIcon,
  PencilIcon,
  TrashIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import { Vurdering } from "~/types/vilkår";
import { useErLesevisning } from "~/hooks/useErLesevisning";

export const VilkårKomponent: React.FC<{
  navn: string;
  beskrivelse: string[];
  valgSpørsmål: string;
  spørsmålSvar: string;
  onChangeSpørsmål: (val: string) => void;
  begrunnelse: string;
  onChangeBegrunnelse: (val: string) => void;
  låst: boolean;
  settLåst: (val: boolean) => void;
  lagrer: boolean;
  onLagre: () => Promise<boolean>;
  onSlett: () => void;
}> = ({
  navn,
  beskrivelse,
  valgSpørsmål,
  spørsmålSvar,
  onChangeSpørsmål,
  begrunnelse,
  onChangeBegrunnelse,
  låst,
  settLåst,
  lagrer = false,
  onLagre,
  onSlett,
}) => {
  const erLesevisning = useErLesevisning();
  const erLåst = låst || erLesevisning;

  const harSvaralternativOgBegrunnelse = spørsmålSvar !== "" && begrunnelse.trim() !== "";
  const handleLagreOgLås = async () => {
    if (onLagre) {
      await onLagre();
    } else {
      settLåst(true);
    }
  };

  const handleKanRedigere = () => {
    settLåst(false);
  };

  const handleTilbakestillVilkår = () => {
    onSlett();
  };

  const erOppfylt = spørsmålSvar === Vurdering.JA;

  const vilkårResultatTekst = erOppfylt ? "Vilkår oppfylt" : "Vilkår ikke oppfylt";

  const vilkårStatusIkon = erOppfylt ? (
    <CheckmarkCircleFillIcon
      title={vilkårResultatTekst.toLowerCase()}
      fontSize="1.5rem"
      color="var(--ax-bg-success-strong)"
    />
  ) : (
    <XMarkOctagonFillIcon
      title={vilkårResultatTekst.toLowerCase()}
      fontSize="1.5rem"
      color="var(--ax-bg-danger-strong)"
    />
  );

  return (
    <Box
      className={styles.container}
      shadow="dialog"
      background="neutral-soft"
      padding={"space-24"}
      borderRadius="4"
    >
      <div className={styles.venstreKolonne}>
        <VStack gap="space-24">
          <Heading size="small">{navn}</Heading>

          <VStack>
            {beskrivelse.map((tekst, index) => (
              <BodyLong size="small" key={index}>
                {tekst}
              </BodyLong>
            ))}
          </VStack>
        </VStack>
      </div>

      <div className={styles.høyreKolonne}>
        <VStack gap="space-24">
          {låst && (
            <HStack gap="space-6" align="center" justify="space-between">
              <HStack gap="space-8" align="center">
                <Heading size="small">{vilkårResultatTekst}</Heading>
                {vilkårStatusIkon}
              </HStack>

              <HStack
                gap="space-2"
                className={erLesevisning ? styles.gjemtILesevisning : undefined}
              >
                <Button
                  variant="tertiary"
                  size="small"
                  icon={<PencilIcon title="Rediger" />}
                  onClick={handleKanRedigere}
                >
                  Rediger
                </Button>
                <Button
                  variant="tertiary"
                  size="small"
                  icon={<TrashIcon title="slett" fontSize="1.5rem" />}
                  onClick={handleTilbakestillVilkår}
                >
                  Slett
                </Button>
              </HStack>
            </HStack>
          )}

          <RadioGroup
            legend={valgSpørsmål}
            onChange={onChangeSpørsmål}
            value={spørsmålSvar}
            readOnly={erLåst}
          >
            <Stack
              gap="space-0 space-24"
              direction={{ xs: "column", sm: "row" }}
              wrap={false}
              style={{ marginTop: "1rem" }}
            >
              <Radio value="JA">Ja</Radio>
              <Radio value="NEI">Nei</Radio>
            </Stack>
          </RadioGroup>

          <Textarea
            label="Begrunnelse"
            onChange={(e) => onChangeBegrunnelse(e.target.value)}
            value={begrunnelse}
            readOnly={erLåst}
          />

          <div>
            <Button
              onClick={handleLagreOgLås}
              disabled={!harSvaralternativOgBegrunnelse || erLåst}
              loading={lagrer}
            >
              Lagre
            </Button>
          </div>
        </VStack>
      </div>
    </Box>
  );
};

export default VilkårKomponent;
