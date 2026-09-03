import React from "react";
import { Button, HStack } from "@navikt/ds-react";
import { PencilIcon, TrashIcon } from "@navikt/aksel-icons";

interface RedigerOgSlettKnapperProps {
  onRediger: () => void;
  onSlett: () => void;
}

export const RedigerOgSlettKnapper: React.FC<RedigerOgSlettKnapperProps> = ({
  onRediger,
  onSlett,
}) => {
  return (
    <HStack gap="space-2" style={{ position: "absolute", top: 0, right: 0, zIndex: 1 }}>
      <Button
        variant="tertiary"
        size="small"
        icon={<PencilIcon title="Rediger" />}
        onClick={onRediger}
      >
        Rediger
      </Button>
      <Button
        variant="tertiary"
        size="small"
        icon={<TrashIcon title="Slett" fontSize="1.5rem" />}
        onClick={onSlett}
      >
        Slett
      </Button>
    </HStack>
  );
};
