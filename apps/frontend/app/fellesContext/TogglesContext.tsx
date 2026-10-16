import React, { createContext, useEffect, useState } from "react";
import { hentFeatureToggles } from "~/api/generated/sdk.gen";
import { type Toggles } from "~/types/toggles";

export interface ToggleContext {
  toggles: Toggles;
  laster: boolean;
}

export const ToggleContext = createContext<ToggleContext | undefined>(undefined);

export const TogglesProvider: React.FC<{
  children: React.ReactNode;
}> = ({ children }) => {
  const [toggles, settToggles] = useState<Toggles>({});
  const [laster, settLaster] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    const hentToggles = async () => {
      try {
        const { data } = await hentFeatureToggles({ throwOnError: true, signal });
        if (signal.aborted) return;
        settToggles(data || {});
      } catch {
        if (signal.aborted) return;
        console.error("Feil ved henting av toggles");
      } finally {
        if (!signal.aborted) settLaster(false);
      }
    };

    void hentToggles();
    return () => controller.abort();
  }, []);

  const value: ToggleContext = {
    toggles,
    laster,
  };

  return <ToggleContext.Provider value={value}>{children}</ToggleContext.Provider>;
};
