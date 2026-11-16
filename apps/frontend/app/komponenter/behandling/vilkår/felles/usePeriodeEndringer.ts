import { useState } from "react";
import { feilmeldingFra } from "./feilmeldingFra";
import type { ApiKallCallbacks, ApiKallResult } from "./useApiKall";
import type { PerioderResult } from "./usePerioder";

export interface PeriodeEndringer<T, TLagreOptions> {
  lagre: (options: TLagreOptions) => void;
  slett: (periode: T) => void;
  lagrer: boolean;
  sletterId: string | undefined;
  slettefeil: string | undefined;
  // Sann mens et lagre- eller slettekall pågår. Endre, Slett og Legg til låses da.
  opptatt: boolean;
}

// Feltene lista og knappene bruker. Uten lagre-typen, så varians mellom skjemaene ikke står i veien.
export type PeriodeEndringerVisning<T> = Pick<
  PeriodeEndringer<T, unknown>,
  "slett" | "sletterId" | "slettefeil" | "opptatt"
>;

/**
 * Lagring og sletting av vilkårsperioder og trygdetid. Hooken eier slettestatusen og
 * oppdaterer lista og skjemaet etter kallene.
 */
export function usePeriodeEndringer<T extends { id: string }, TLagreOptions>(args: {
  perioder: PerioderResult<T>;
  skjema: { lukk: () => void; settBackendfeil: (melding: string | undefined) => void };
  lagre: ApiKallResult<TLagreOptions>;
  slett: (periodeId: string, callbacks: ApiKallCallbacks) => boolean;
  lagrefeilTekst: string;
}): PeriodeEndringer<T, TLagreOptions> {
  const { perioder, skjema, lagre: lagreKall, slett: slettKall, lagrefeilTekst } = args;
  const [sletterId, settSletterId] = useState<string | undefined>(undefined);
  const [slettefeil, settSlettefeil] = useState<string | undefined>(undefined);

  const lagrer = lagreKall.laster;
  const opptatt = lagrer || sletterId !== undefined;

  const lagre = (options: TLagreOptions) => {
    if (opptatt) return;

    lagreKall.kall(options, {
      onSuccess: () => {
        settSlettefeil(undefined);
        skjema.lukk();
        perioder.hentPåNytt();
      },
      // Skjemaet blir stående åpent ved feil, så saksbehandler kan rette og prøve igjen.
      onError: (error) => skjema.settBackendfeil(feilmeldingFra(error, lagrefeilTekst)),
    });
  };

  const slett = (periode: T) => {
    if (opptatt) return;

    const startet = slettKall(periode.id, {
      onSuccess: () => perioder.hentPåNytt(),
      onError: (error) => settSlettefeil(feilmeldingFra(error, "Kunne ikke slette perioden.")),
      onSettled: () => settSletterId(undefined),
    });
    if (!startet) return;

    settSletterId(periode.id);
    settSlettefeil(undefined);
  };

  return { lagre, slett, lagrer, sletterId, slettefeil, opptatt };
}
