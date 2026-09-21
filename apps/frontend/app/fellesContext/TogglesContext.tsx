import React, { createContext, useEffect, useState } from "react";
import { apiCall } from "~/api/backend";
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
    void apiCall<Record<string, boolean>>("/unleash/toggles").then((response) => {
      if (response.status) {
        console.error("Feil ved henting av toggles:", response.status);
      } else {
        settToggles(response.data || {});
      }
      settLaster(false);
    });
  }, []);

  const value: ToggleContext = {
    toggles,
    laster,
  };

  return <ToggleContext.Provider value={value}>{children}</ToggleContext.Provider>;
};
