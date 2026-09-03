import { useContext } from "react";
import { ToggleContext } from "~/contexts/TogglesContext";

export const useToggles = () => {
  const context = useContext(ToggleContext);

  if (context === undefined) {
    throw new Error("useToggles må brukes innenfor en TogglesProvider");
  }

  return context;
};
