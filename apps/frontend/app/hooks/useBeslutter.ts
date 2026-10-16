import { useState } from "react";
import {
  angreSendTilBeslutter as angreSendTilBeslutterKall,
  besluttVedtak as besluttVedtakKall,
  sendTilBeslutter as sendTilBeslutterKall,
} from "~/api/generated/sdk.gen";
import type { BeslutteVedtakRequest } from "~/api/generated/types.gen";
import { feilmeldingFra } from "~/komponenter/behandling/vilkår/felles/feilmeldingFra";

export const useBeslutter = () => {
  const [sender, settSender] = useState(false);

  const sendTilBeslutter = async (behandlingId: string) => {
    settSender(true);
    try {
      const { data } = await sendTilBeslutterKall({
        path: { behandlingId },
        throwOnError: true,
      });
      return { data };
    } catch (feil: unknown) {
      return { feilmelding: feilmeldingFra(feil, "Kunne ikke sende vedtak til beslutter.") };
    } finally {
      settSender(false);
    }
  };

  const angreSendTilBeslutter = async (behandlingId: string) => {
    settSender(true);
    try {
      const { data } = await angreSendTilBeslutterKall({
        path: { behandlingId },
        throwOnError: true,
      });
      return { data };
    } catch (feil: unknown) {
      return { feilmelding: feilmeldingFra(feil, "Kunne ikke angre sending til beslutter.") };
    } finally {
      settSender(false);
    }
  };

  const besluttVedtak = async (behandlingId: string, body: BeslutteVedtakRequest) => {
    settSender(true);
    try {
      const { data } = await besluttVedtakKall({
        path: { behandlingId },
        body,
        throwOnError: true,
      });
      return { data };
    } catch (feil: unknown) {
      return { feilmelding: feilmeldingFra(feil, "Kunne ikke beslutte vedtak.") };
    } finally {
      settSender(false);
    }
  };

  return {
    sender,
    sendTilBeslutter,
    angreSendTilBeslutter,
    besluttVedtak,
  };
};
