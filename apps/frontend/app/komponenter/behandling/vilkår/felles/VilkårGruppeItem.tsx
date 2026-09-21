import React from "react";
import { Accordion, Button, HStack, Tag, VStack } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";

export function VilkårGruppeItem({
  tittel,
  erOppfylt,
  defaultOpen,
  onLeggTil,
  children,
}: {
  tittel: string;
  erOppfylt: boolean;
  defaultOpen: boolean;
  onLeggTil?: () => void;
  children: React.ReactNode;
}) {
  const status = erOppfylt
    ? { tekst: "Oppfylt", farge: "success" as const }
    : { tekst: "Ikke oppfylt", farge: "danger" as const };

  return (
    <Accordion.Item defaultOpen={defaultOpen}>
      <Accordion.Header>
        <HStack gap="space-12" align="center" wrap={false}>
          <span>{tittel}</span>
          <Tag variant="strong" size="small" data-color={status.farge}>
            {status.tekst}
          </Tag>
        </HStack>
      </Accordion.Header>

      <Accordion.Content>
        <VStack gap="space-16">
          {children}

          {onLeggTil && (
            <div>
              <Button
                type="button"
                variant="secondary"
                icon={<PlusIcon aria-hidden />}
                onClick={onLeggTil}
              >
                Legg til periode
              </Button>
            </div>
          )}
        </VStack>
      </Accordion.Content>
    </Accordion.Item>
  );
}
