import React from "react";
import { Tabs, BodyShort, Skeleton, VStack } from "@navikt/ds-react";
import { Endringshistorikk } from "~/komponenter/behandling/høyremeny/Endringshistorikk";
import { Dokumentliste } from "~/komponenter/dokumentoversikt/Dokumentliste";
import { usePersonContext } from "~/contexts/PersonContext";
import { useHentDokumenter } from "~/hooks/useHentDokumenter";
import styles from "./SidebarTabs.module.css";

const DokumenterTab = () => {
  const { fagsakPersonId } = usePersonContext();
  const { dokumenter, laster } = useHentDokumenter(fagsakPersonId);

  if (laster) {
    return (
      <VStack gap="space-8">
        <Skeleton variant="text" width="80%" />
        <Skeleton variant="text" width="60%" />
        <Skeleton variant="text" width="70%" />
        <Skeleton variant="text" width="50%" />
      </VStack>
    );
  }

  if (!dokumenter) {
    return (
      <BodyShort size="small" textColor="subtle">
        Kunne ikke hente dokumenter
      </BodyShort>
    );
  }

  return <Dokumentliste dokumenter={dokumenter} medScroll={false} />;
};

export const SidebarTabs = () => {
  return (
    <Tabs defaultValue="historikk" size="small" className={styles.sidebarTabs}>
      <Tabs.List>
        <Tabs.Tab value="historikk" label="Historikk" />
        <Tabs.Tab value="dokumenter" label="Dokumenter" />
      </Tabs.List>
      <Tabs.Panel value="historikk" className={styles.tabPanel}>
        <Endringshistorikk />
      </Tabs.Panel>
      <Tabs.Panel value="dokumenter" className={styles.tabPanel}>
        <DokumenterTab />
      </Tabs.Panel>
    </Tabs>
  );
};
