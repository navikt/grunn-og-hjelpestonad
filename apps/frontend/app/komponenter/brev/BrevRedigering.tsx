import React, { useState } from "react";
import { Button, Heading, Modal, Select, VStack } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import { brevmaler } from "~/komponenter/brev/brevmaler";
import type { Brevmal, Tekstbolk } from "~/komponenter/brev/typer";
import { Fritekstbolk } from "~/komponenter/brev/Fritekstbolk";
import BrevmottakerModalInnhold from "~/komponenter/brev/BrevMottakerModal";
import type { Brevmottaker } from "~/hooks/useBrevmottaker";
import { useErLesevisning } from "~/hooks/useErLesevisning";

interface Props {
  brevMal: Brevmal | null;
  fritekstbolker: Tekstbolk[];
  velgBrevmal: (brevmal: string) => void;
  oppdaterFelt: (index: number, partial: Partial<Tekstbolk>) => void;
  flyttBolkOpp: (index: number) => void;
  flyttBolkNed: (index: number) => void;
  slettFritekstbolk: (index: number) => void;
  leggTilFritekstbolk: () => void;
  mottakere: Brevmottaker[];
  settMottakere: (mottakere: Brevmottaker[]) => void;
  utledBrevmottakere: () => string;
  sendMottakereTilSak: (behandlingId: string, mottakere: Brevmottaker[]) => void;
  className?: string;
}

export function BrevRedigering({
  brevMal,
  fritekstbolker,
  velgBrevmal,
  oppdaterFelt,
  flyttBolkOpp,
  flyttBolkNed,
  slettFritekstbolk,
  leggTilFritekstbolk,
  mottakere,
  settMottakere,
  utledBrevmottakere,
  sendMottakereTilSak,
  className,
}: Props) {
  const erLesevisning = useErLesevisning();
  const [modalÅpen, settModalÅpen] = useState(false);

  return (
    <>
      <VStack gap="space-24" overflow="auto" minHeight="0" flexGrow="1" className={className}>
        <Heading level="1" size="small">
          Brevmottaker: {utledBrevmottakere()}
        </Heading>
        <div>
          <Button variant="secondary" size="small" onClick={() => settModalÅpen(true)} disabled={erLesevisning}>
            Legg til/endre brevmottaker
          </Button>
        </div>
        <Select
          label="Velg dokument"
          value={brevMal?.tittel ?? ""}
          onChange={(e) => velgBrevmal(e.target.value)}
          size="medium"
          disabled={erLesevisning}
        >
          <option value="" disabled>
            Ikke valgt
          </option>
          {brevmaler.map((mal) => (
            <option key={mal.tittel} value={mal.tittel}>
              {mal.tittel}
            </option>
          ))}
        </Select>

        {brevMal && (
          <>
            <Heading level="3" size="xsmall">
              Fritekstområde
            </Heading>
            {fritekstbolker.map((fritekstfelt, index) => (
              <Fritekstbolk
                key={index}
                underoverskrift={fritekstfelt.underoverskrift}
                innhold={fritekstfelt.innhold}
                handleOppdaterFelt={(partial) => oppdaterFelt(index, partial)}
                handleFlyttOpp={() => flyttBolkOpp(index)}
                handleFlyttNed={() => flyttBolkNed(index)}
                handleSlett={() => slettFritekstbolk(index)}
                fritekstfeltListe={fritekstbolker}
              />
            ))}
            <Button
              variant="tertiary"
              icon={<PlusIcon title="Legg til fritekstfelt" />}
              onClick={leggTilFritekstbolk}
              disabled={erLesevisning}
            >
              Legg til fritekstfelt
            </Button>
          </>
        )}
      </VStack>

      <Modal
        open={modalÅpen}
        onClose={() => settModalÅpen(false)}
        header={{ heading: "Hvem skal motta brevet?" }}
        width="50rem"
      >
        <BrevmottakerModalInnhold
          mottakere={mottakere}
          settMottakere={settMottakere}
          lukkModal={() => settModalÅpen(false)}
          sendMottakereTilSak={sendMottakereTilSak}
        />
      </Modal>
    </>
  );
}
