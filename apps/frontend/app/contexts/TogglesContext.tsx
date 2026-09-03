import React, { createContext, useEffect, useState } from "react";
import { apiCall, type ApiResponse } from "~/api/backend";
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

  const fetchToggles = async () => {
    const hentToggles = async (): Promise<ApiResponse<Record<string, boolean>>> => {
      return apiCall("/unleash/toggles");
    };

    settLaster(true);
    const response = await hentToggles();

    if (response.status) {
      console.error("Feil ved henting av toggles:", response.status);
    } else {
      settToggles(response.data || {});
    }
    settLaster(false);
  };

  useEffect(() => {
    fetchToggles();
  }, []);

  const value: ToggleContext = {
    toggles,
    laster,
  };

  return <ToggleContext.Provider value={value}>{children}</ToggleContext.Provider>;
};
