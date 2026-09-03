import React from "react";
import {
  Alert,
  BodyShort,
  Button,
  Detail,
  HStack,
  Loader,
  Popover,
  VStack,
} from "@navikt/ds-react";
import { PersonIcon } from "@navikt/aksel-icons";
import type { Søkeresultat } from "~/hooks/useSøk";
import { useTemaContext } from "~/contexts/TemaContext";
import styles from "./SøkePopover.module.css";
import { beregnAlder } from "~/utils/utils";

interface SøkePopoverProps {
  open: boolean;
  onClose: () => void;
  anchorEl: HTMLDivElement | null;
  søker: boolean;
  feilmelding: string | null;
  søkeresultat: Søkeresultat | null;
  onNavigate: (fagsakPersonId: string) => void;
  onOpprettFagsak: () => void;
  opprettFeilmelding?: string | null;
}

export const SøkePopover: React.FC<SøkePopoverProps> = ({
  open,
  onClose,
  anchorEl,
  søker,
  feilmelding,
  søkeresultat,
  onNavigate,
  onOpprettFagsak,
  opprettFeilmelding,
}) => {
  const { mørktTema } = useTemaContext();
  const visFeilmelding = feilmelding || opprettFeilmelding;
  const minBredde = anchorEl?.offsetWidth ? `${anchorEl.offsetWidth}px` : undefined;
  const temaKlasse = mørktTema ? "dark" : "light";

  const alder = beregnAlder(søkeresultat?.fødselsdato);

  return (
    <Popover
      open={open}
      onClose={onClose}
      anchorEl={anchorEl}
      placement="bottom"
      className={temaKlasse}
      data-color="accent"
    >
      <Popover.Content style={{ minWidth: minBredde }}>
        {søker && !søkeresultat ? (
          <HStack gap="space-4" align="center" justify="center" padding="space-2">
            <Loader size="small" />
            <BodyShort size="small">Søker...</BodyShort>
          </HStack>
        ) : visFeilmelding ? (
          <Alert variant="warning" size="small" inline>
            {visFeilmelding}
          </Alert>
        ) : søkeresultat ? (
          <VStack gap="space-4">
            <HStack gap="space-4" align="center">
              <PersonIcon className={styles.personIkon} aria-hidden />
              <div>
                <BodyShort size="small" weight="semibold">
                  {søkeresultat.navn}
                  {alder !== null ? ` (${alder})` : ""}
                </BodyShort>
                <Detail textColor="subtle">
                  {søkeresultat.personident || søkeresultat.fagsakPersonId}
                </Detail>
              </div>
            </HStack>
            {søkeresultat.harFagsak ? (
              <Button
                size="small"
                onClick={() => onNavigate(søkeresultat.fagsakPersonId)}
                disabled={søker}
                style={{ width: "100%" }}
              >
                Gå til oversikt
              </Button>
            ) : (
              <Button
                variant="secondary"
                size="small"
                onClick={onOpprettFagsak}
                disabled={søker}
                loading={søker}
                style={{ width: "100%" }}
              >
                Opprett fagsak
              </Button>
            )}
          </VStack>
        ) : null}
      </Popover.Content>
    </Popover>
  );
};
