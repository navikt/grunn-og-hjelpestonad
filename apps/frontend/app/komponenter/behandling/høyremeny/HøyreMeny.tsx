import React from "react";
import styles from "./HøyreMeny.module.css";
import { VStack } from "@navikt/ds-react";

export const HøyreMeny: React.FC<{
  children?: React.ReactNode;
}> = ({ children }) => {
  return (
    <VStack as={"aside"} className={styles.høyreMeny} gap={"space-16"}>
      {children}
    </VStack>
  );
};
