import React from "react";
import type { Dokumentinfo } from "~/api/dokument";
import { DokumentListeElement } from "~/komponenter/dokumentoversikt/DokumentListeElement";
import { BodyShort } from "@navikt/ds-react";
import { FilesIcon } from "@navikt/aksel-icons";
import styles from "./Dokumentliste.module.css";

export interface Props {
  dokumenter: Dokumentinfo[];
  medScroll: boolean;
}

export const Dokumentliste: React.FC<Props> = ({ dokumenter, medScroll = false }) => {
  if (dokumenter.length === 0) {
    return (
      <div className={styles.tomtResultat}>
        <FilesIcon className={styles.tomtResultatIkon} aria-hidden />
        <BodyShort size="small" textColor="subtle">
          Ingen dokumenter funnet
        </BodyShort>
      </div>
    );
  }

  return (
    <div className={`${styles.dokumentListe}${medScroll ? ` ${styles.dokumentListeMedScroll}` : ""}`}>
      {dokumenter.map((dokument: Dokumentinfo) => (
        <DokumentListeElement
          key={dokument.dokumentinfoId}
          dokument={dokument}
        />
      ))}
    </div>
  );
};
