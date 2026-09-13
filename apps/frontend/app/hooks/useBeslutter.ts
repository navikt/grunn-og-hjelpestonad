import { useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
import type { BeslutteVedtakDto } from "~/api/generated/types.gen";

export const useBeslutter = () => {
  const [sender, settSender] = useState(false);

  const kallBeslutterEndepunkt = async (
    endpoint: string,
    behandlingId: string,
    body?: unknown
  ): Promise<ApiResponse> => {
    settSender(true);
    try {
      return await apiCall(`/beslutter/${endpoint}/${behandlingId}`, {
        method: "POST",
        body: body ? JSON.stringify(body) : undefined,
      });
    } finally {
      settSender(false);
    }
  };

  const sendTilBeslutter = (behandlingId: string) =>
    kallBeslutterEndepunkt("send-til-beslutter", behandlingId);

  const angreSendTilBeslutter = (behandlingId: string) =>
    kallBeslutterEndepunkt("angre-send-til-beslutter", behandlingId);

  const besluttVedtak = (behandlingId: string, beslutteVedtakDto: BeslutteVedtakDto) =>
    kallBeslutterEndepunkt("beslutt-vedtak", behandlingId, beslutteVedtakDto);

  return {
    sender,
    sendTilBeslutter,
    angreSendTilBeslutter,
    besluttVedtak,
  };
};
