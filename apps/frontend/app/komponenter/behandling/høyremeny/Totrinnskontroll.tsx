import React, { useState } from "react";
import {
  Alert,
  BodyLong,
  BodyShort,
  Box,
  Button,
  Heading,
  InlineMessage,
  Radio,
  RadioGroup,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import { useBeslutter } from "~/hooks/useBeslutter";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";
import { InfoRad } from "./InfoRad";
import {
  TotrinnskontrollStatus,
  ÅrsakUnderkjent,
  årsakUnderkjentTekst,
} from "~/types/totrinnskontroll";
import { formaterIsoDatoTid } from "~/utils/utils";
import type { AnsvarligSaksbehandlerDto } from "~/types/saksbehandler";

enum Totrinnsresultat {
  IKKE_VALGT = "IKKE_VALGT",
  GODKJENT = "GODKJENT",
  UNDERKJENT = "UNDERKJENT",
}

export const Totrinnskontroll = () => {
  const {
    behandling,
    revaliderBehandling,
    behandlingId,
    totrinnskontrollStatus,
    hentTotrinnskontrollStatusPåNytt,
    hentAnsvarligSaksbehandlerPåNytt,
    ansvarligSaksbehandler,
  } = useBehandlingContext();
  const { angreSendTilBeslutter, besluttVedtak, sender } = useBeslutter();

  const erSendtTilBeslutter = behandling?.status === "FATTER_VEDTAK";

  if (
    !erSendtTilBeslutter &&
    totrinnskontrollStatus?.status !== TotrinnskontrollStatus.TOTRINNSKONTROLL_UNDERKJENT
  ) {
    return null;
  }

  if (totrinnskontrollStatus?.status === TotrinnskontrollStatus.TOTRINNSKONTROLL_UNDERKJENT) {
    return <TotrinnskontrollUnderkjent totrinnskontrollStatus={totrinnskontrollStatus} />;
  }

  if (totrinnskontrollStatus?.status === TotrinnskontrollStatus.KAN_FATTE_VEDTAK) {
    return (
      <FatterVedtak
        behandlingId={behandlingId}
        besluttVedtak={besluttVedtak}
        sender={sender}
        revaliderBehandling={revaliderBehandling}
        hentTotrinnskontrollStatusPåNytt={hentTotrinnskontrollStatusPåNytt}
        hentAnsvarligSaksbehandlerPåNytt={hentAnsvarligSaksbehandlerPåNytt}
        ansvarligSaksbehandler={ansvarligSaksbehandler}
      />
    );
  }

  if (totrinnskontrollStatus?.status === TotrinnskontrollStatus.IKKE_AUTORISERT) {
    return (
      <SendtTilBeslutter
        behandlingId={behandlingId}
        angreSendTilBeslutter={angreSendTilBeslutter}
        revaliderBehandling={revaliderBehandling}
        hentTotrinnskontrollStatusPåNytt={hentTotrinnskontrollStatusPåNytt}
        hentAnsvarligSaksbehandlerPåNytt={hentAnsvarligSaksbehandlerPåNytt}
        opprettetAv={totrinnskontrollStatus.totrinnskontroll?.opprettetAv}
        opprettetTid={totrinnskontrollStatus.totrinnskontroll?.opprettetTid}
      />
    );
  }

  return null;
};

const SendtTilBeslutter: React.FC<{
  behandlingId: string;
  angreSendTilBeslutter: (behandlingId: string) => Promise<{ data?: unknown }>;
  revaliderBehandling: () => void;
  hentTotrinnskontrollStatusPåNytt: () => void;
  hentAnsvarligSaksbehandlerPåNytt: () => void;
  opprettetAv?: string;
  opprettetTid?: string;
}> = ({
  behandlingId,
  angreSendTilBeslutter,
  revaliderBehandling,
  hentTotrinnskontrollStatusPåNytt,
  hentAnsvarligSaksbehandlerPåNytt,
  opprettetAv,
  opprettetTid,
}) => {
  const { ansvarligSaksbehandler } = useBehandlingContext();
  const [laster, settLaster] = useState(false);

  const erAnsvarligSaksbehandler = ansvarligSaksbehandler?.rolle === "INNLOGGET_SAKSBEHANDLER";
  const behandlingHarIngenAnsvarligSaksbehandler = ansvarligSaksbehandler === null;

  const handleAngreSendTilBeslutter = async () => {
    settLaster(true);
    try {
      const respons = await angreSendTilBeslutter(behandlingId);
      if (respons.data) {
        oppdaterEndringshistorikk();
        revaliderBehandling();
        hentTotrinnskontrollStatusPåNytt();
        hentAnsvarligSaksbehandlerPåNytt();
      }
    } finally {
      settLaster(false);
    }
  };

  return (
    <Box borderColor="neutral" borderWidth="1" borderRadius="12" padding="space-16">
      <VStack gap="space-12">
        <Heading size="xsmall">Totrinnskontroll</Heading>
        <InlineMessage status="info" size="small">
          Vedtaket er sendt til godkjenning
        </InlineMessage>
        <VStack gap="space-4">
          <InfoRad label="Sendt inn av" verdi={opprettetAv || ""} />
          <InfoRad label="Sendt inn" verdi={opprettetTid || ""} />
        </VStack>
        <Button
          size="small"
          variant="secondary"
          onClick={handleAngreSendTilBeslutter}
          loading={laster}
          disabled={!erAnsvarligSaksbehandler || behandlingHarIngenAnsvarligSaksbehandler}
        >
          Angre send til beslutter
        </Button>
      </VStack>
    </Box>
  );
};

const FatterVedtak: React.FC<{
  behandlingId: string;
  besluttVedtak: (
    behandlingId: string,
    data: { godkjent: boolean; årsakUnderkjent?: ÅrsakUnderkjent; begrunnelse?: string }
  ) => Promise<{ data?: unknown; feilmelding?: string }>;
  sender: boolean;
  revaliderBehandling: () => void;
  hentTotrinnskontrollStatusPåNytt: () => void;
  hentAnsvarligSaksbehandlerPåNytt: () => void;
  ansvarligSaksbehandler: AnsvarligSaksbehandlerDto | null;
}> = ({
  behandlingId,
  besluttVedtak,
  sender,
  revaliderBehandling,
  hentTotrinnskontrollStatusPåNytt,
  hentAnsvarligSaksbehandlerPåNytt,
  ansvarligSaksbehandler,
}) => {
  const [totrinnsresultat, settTotrinnsresultat] = useState<Totrinnsresultat>(
    Totrinnsresultat.IKKE_VALGT
  );
  const [årsakUnderkjent, settÅrsakUnderkjent] = useState<ÅrsakUnderkjent | null>(null);
  const [begrunnelse, settBegrunnelse] = useState("");
  const [feilmelding, settFeilmelding] = useState<string | null>(null);

  const erGodkjent = totrinnsresultat === Totrinnsresultat.GODKJENT;
  const erUnderkjent = totrinnsresultat === Totrinnsresultat.UNDERKJENT;
  const erUtfylt =
    erGodkjent || (erUnderkjent && årsakUnderkjent !== null && begrunnelse.trim() !== "");
  const erAnsvarligSaksbehandler = ansvarligSaksbehandler?.rolle === "INNLOGGET_SAKSBEHANDLER";

  const handleFullfør = async () => {
    settFeilmelding(null);
    const respons = await besluttVedtak(behandlingId, {
      godkjent: erGodkjent,
      ...(erUnderkjent && {
        årsakUnderkjent: årsakUnderkjent!,
        begrunnelse: begrunnelse.trim(),
      }),
    });

    if (respons.data) {
      oppdaterEndringshistorikk();
      revaliderBehandling();
      hentTotrinnskontrollStatusPåNytt();
      hentAnsvarligSaksbehandlerPåNytt();
    } else if (respons.feilmelding) {
      settFeilmelding(respons.feilmelding);
    }
  };

  return (
    <Box borderColor="neutral" borderWidth="1" borderRadius="12" padding="space-16">
      <VStack gap="space-12">
        <Heading size="xsmall">Totrinnskontroll</Heading>
        <RadioGroup
          legend="Beslutt vedtak"
          value={totrinnsresultat}
          onChange={(val) => settTotrinnsresultat(val as Totrinnsresultat)}
          size="small"
          disabled={!erAnsvarligSaksbehandler}
        >
          <Radio value={Totrinnsresultat.GODKJENT}>Godkjenn</Radio>
          <Radio value={Totrinnsresultat.UNDERKJENT}>Underkjenn</Radio>
        </RadioGroup>

        {erUnderkjent && (
          <>
            <RadioGroup
              legend="Årsak"
              value={årsakUnderkjent ?? ""}
              onChange={(val) => settÅrsakUnderkjent(val as ÅrsakUnderkjent)}
              size="small"
            >
              {Object.values(ÅrsakUnderkjent).map((årsak) => (
                <Radio key={årsak} value={årsak}>
                  {årsakUnderkjentTekst[årsak]}
                </Radio>
              ))}
            </RadioGroup>
            <Textarea
              label="Begrunnelse"
              value={begrunnelse}
              onChange={(e) => settBegrunnelse(e.target.value)}
              size="small"
            />
          </>
        )}

        {(erGodkjent || erUnderkjent) && (
          <Button size="small" onClick={handleFullfør} loading={sender} disabled={!erUtfylt}>
            Fullfør
          </Button>
        )}

        {feilmelding && (
          <Alert variant="error" size="small">
            {feilmelding}
          </Alert>
        )}
      </VStack>
    </Box>
  );
};

const TotrinnskontrollUnderkjent: React.FC<{
  totrinnskontrollStatus: {
    status: TotrinnskontrollStatus.TOTRINNSKONTROLL_UNDERKJENT;
    totrinnskontroll: {
      opprettetAv: string;
      opprettetTid: string;
      årsakUnderkjent?: ÅrsakUnderkjent;
      begrunnelse?: string;
    };
  };
}> = ({ totrinnskontrollStatus }) => {
  const { totrinnskontroll } = totrinnskontrollStatus;

  return (
    <Box borderColor="neutral" borderWidth="1" borderRadius="12" padding="space-16">
      <VStack gap="space-12">
        <Heading size="xsmall">Totrinnskontroll</Heading>
        <InlineMessage status="warning" size="small">
          Vedtaket er underkjent
        </InlineMessage>
        <VStack gap="space-4">
          <InfoRad label="Underkjent av" verdi={totrinnskontroll.opprettetAv} />
          <InfoRad label="Underkjent" verdi={formaterIsoDatoTid(totrinnskontroll.opprettetTid)} />
          {totrinnskontroll.årsakUnderkjent && (
            <InfoRad
              label="Årsak underkjent"
              verdi={årsakUnderkjentTekst[totrinnskontroll.årsakUnderkjent]}
            />
          )}
        </VStack>
        {totrinnskontroll.begrunnelse && (
          <VStack gap="space-2">
            <BodyShort size="small" weight="semibold">
              Begrunnelse
            </BodyShort>
            <BodyLong size="small">{totrinnskontroll.begrunnelse}</BodyLong>
          </VStack>
        )}
      </VStack>
    </Box>
  );
};
