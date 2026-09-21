import { useCallback, useState } from "react";
import { oppdaterEndringshistorikk } from "~/utils/endringshistorikkEvent";

export type ApiKallCallbacks = {
  onSuccess?: () => void;
  onError?: (error: unknown) => void;
  onSettled?: () => void;
};

export interface VilkårApiKallResult<TOptions> {
  kall: (options: TOptions, callbacks?: ApiKallCallbacks) => void;
  laster: boolean;
}

export function useVilkårApiKall<TOptions>(
  apiKall: (options: TOptions) => Promise<unknown>
): VilkårApiKallResult<TOptions> {
  const [laster, settLaster] = useState(false);

  const kall = useCallback(
    (options: TOptions, callbacks?: ApiKallCallbacks) => {
      settLaster(true);

      void apiKall(options).then(
        () => {
          settLaster(false);
          oppdaterEndringshistorikk();
          callbacks?.onSuccess?.();
          callbacks?.onSettled?.();
        },
        (error: unknown) => {
          settLaster(false);
          callbacks?.onError?.(error);
          callbacks?.onSettled?.();
        }
      );
    },
    [apiKall]
  );

  return { kall, laster };
}
