import React, { useEffect, useRef, useState } from "react";
import { Box, HGrid, VStack } from "@navikt/ds-react";
import type { Route } from "./+types/brev";
import { useBrev } from "~/komponenter/brev/useBrev";
import { useBehandlingContext } from "~/fellesContext/BehandlingContext";
import { useBrevmottaker } from "~/hooks/useBrevmottaker";
import { useStegNavigering } from "~/hooks/useStegNavigering";
import { useBeslutter } from "~/hooks/useBeslutter";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";
import type { StegPath } from "~/komponenter/navbar/BehandlingFaner";
import { BrevRedigering } from "~/komponenter/brev/BrevRedigering";
import { BrevForhåndsvisning } from "~/komponenter/brev/BrevForhåndsvisning";
import { BrevHandlinger } from "~/komponenter/brev/BrevHandlinger";
import { SendTilBeslutterModal } from "~/komponenter/brev/SendTilBeslutterModal";
import { useErLesevisning } from "~/hooks/useErLesevisning";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";
import styles from "./brev.module.css";

export function meta(_args: Route.MetaArgs) {
  return [
    { title: "Brev" },
    {
      name: "Brev",
      content: "Brevside",
    },
  ];
}

const STEG_PATH: StegPath = "brev";

export default function Brev() {
  const {
    behandlingId,
    revaliderBehandling,
    behandling,
    hentTotrinnskontrollStatusPåNytt,
    hentAnsvarligSaksbehandlerPåNytt,
  } = useBehandlingContext();
  const { mottakere, settMottakere, utledBrevmottakere, sendMottakereTilSak } =
    useBrevmottaker(behandlingId);

  const {
    brevMal,
    fritekstbolker,
    sender,
    leggTilFritekstbolk,
    flyttBolkOpp,
    flyttBolkNed,
    oppdaterFelt,
    velgBrevmal,
    sendPdfTilSak,
    mellomlagreBrev,
    slettFritekstbolk,
  } = useBrev(behandlingId);

  const erSendtTilBeslutter = behandling?.status === "FATTER_VEDTAK";
  const { sendTilBeslutter } = useBeslutter();
  const [senderTilBeslutter, settSenderTilBeslutter] = useState(false);
  const [sendTilBeslutterFeil, settSendTilBeslutterFeil] = useState<string | null>(null);
  const { navigerTilForrige, harForrigeSteg } = useStegNavigering(STEG_PATH);
  const bekreftModalRef = useRef<HTMLDialogElement>(null);
  const erLesevisning = useErLesevisning();

  useEffect(() => {
    if (!brevMal || erLesevisning || senderTilBeslutter) return;
    const timer = setTimeout(() => {
      mellomlagreBrev(behandlingId, brevMal, fritekstbolker);
    }, 5000);

    return () => clearTimeout(timer);
  }, [behandlingId, brevMal, fritekstbolker, mellomlagreBrev, erLesevisning, senderTilBeslutter]);

  const handleSendTilBeslutter = async () => {
    if (senderTilBeslutter) return;
    settSendTilBeslutterFeil(null);
    if (!brevMal) {
      settSendTilBeslutterFeil("Velg en brevmal før du sender til beslutter.");
      return;
    }

    settSenderTilBeslutter(true);
    try {
      await mellomlagreBrev(behandlingId, brevMal, fritekstbolker);

      const respons = await sendTilBeslutter(behandlingId);
      if (!respons.data) {
        settSendTilBeslutterFeil(
          respons.feilmelding ?? "Kunne ikke sende behandlingen til beslutter. Prøv igjen."
        );
        return;
      }

      bekreftModalRef.current?.close();
      oppdaterEndringshistorikk();
      revaliderBehandling();
      hentTotrinnskontrollStatusPåNytt();
      hentAnsvarligSaksbehandlerPåNytt();
    } catch (feil: unknown) {
      settSendTilBeslutterFeil(
        feil instanceof Error
          ? feil.message
          : feilmeldingFra(feil, "Kunne ikke sende behandlingen til beslutter. Prøv igjen.")
      );
    } finally {
      settSenderTilBeslutter(false);
    }
  };

  const handleSendTilBeslutterKlikk = () => {
    settSendTilBeslutterFeil(null);
    bekreftModalRef.current?.showModal();
  };

  return (
    <VStack className={styles.side} gap="space-24">
      <Box
        shadow="dialog"
        background="neutral-soft"
        padding="space-24"
        borderRadius="4"
        overflow="hidden"
        className={styles.brevBoks}
      >
        <HGrid
          columns="5fr 7fr"
          gap="space-24"
          minHeight="0"
          flexGrow="1"
          className={styles.innholdGrid}
        >
          <BrevRedigering
            brevMal={brevMal}
            fritekstbolker={fritekstbolker}
            velgBrevmal={velgBrevmal}
            oppdaterFelt={oppdaterFelt}
            flyttBolkOpp={flyttBolkOpp}
            flyttBolkNed={flyttBolkNed}
            slettFritekstbolk={slettFritekstbolk}
            leggTilFritekstbolk={leggTilFritekstbolk}
            mottakere={mottakere}
            settMottakere={settMottakere}
            utledBrevmottakere={utledBrevmottakere}
            sendMottakereTilSak={sendMottakereTilSak}
            className={`${styles.fokusringPadding} ${styles.venstreKolonne}`}
          />
          <BrevForhåndsvisning brevMal={brevMal} fritekstbolker={fritekstbolker} />
        </HGrid>
      </Box>

      <BrevHandlinger
        brevMal={brevMal}
        fritekstbolker={fritekstbolker}
        behandlingId={behandlingId}
        sender={sender}
        sendPdfTilSak={sendPdfTilSak}
        senderTilBeslutter={senderTilBeslutter}
        handleSendTilBeslutter={handleSendTilBeslutterKlikk}
        erSendtTilBeslutter={erSendtTilBeslutter}
        harForrigeSteg={harForrigeSteg}
        navigerTilForrige={navigerTilForrige}
      />

      <SendTilBeslutterModal
        modalRef={bekreftModalRef}
        laster={senderTilBeslutter}
        feilmelding={sendTilBeslutterFeil}
        onSendTilBeslutter={handleSendTilBeslutter}
      />
    </VStack>
  );
}
