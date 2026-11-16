import { useCallback, useRef, useState } from "react";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";

export type ApiKallCallbacks = {
  onSuccess?: () => void;
  onError?: (error: unknown) => void;
  onSettled?: () => void;
};

export interface ApiKallResult<TOptions> {
  kall: (options: TOptions, callbacks?: ApiKallCallbacks) => boolean;
  laster: boolean;
}

export function useApiKall<TOptions>(
  apiKall: (options: TOptions) => Promise<unknown>
): ApiKallResult<TOptions> {
  const [laster, settLaster] = useState(false);
  // laster er state og oppdateres først ved neste render. Et raskt dobbeltklikk kan derfor
  // starte to kall før laster er true, så ref-en settes synkront og blokkerer det andre kallet.
  const pågår = useRef(false);

  const kall = useCallback(
    (options: TOptions, callbacks?: ApiKallCallbacks) => {
      if (pågår.current) return false;

      pågår.current = true;
      settLaster(true);

      // Promise-konstruktøren gjør et synkront kast om til avvisning, så pågår alltid nullstilles.
      void new Promise((resolve) => resolve(apiKall(options))).then(
        () => {
          pågår.current = false;
          settLaster(false);
          oppdaterEndringshistorikk();
          callbacks?.onSuccess?.();
          callbacks?.onSettled?.();
        },
        (error: unknown) => {
          pågår.current = false;
          settLaster(false);
          callbacks?.onError?.(error);
          callbacks?.onSettled?.();
        }
      );

      return true;
    },
    [apiKall]
  );

  return { kall, laster };
}
