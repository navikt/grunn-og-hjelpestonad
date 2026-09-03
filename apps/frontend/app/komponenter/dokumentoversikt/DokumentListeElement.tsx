import React from "react";
import type { Dokumentinfo } from "~/api/dokument";
import { BodyShort, Detail, Tag, Tooltip } from "@navikt/ds-react";
import {
  InboxDownIcon,
  PaperplaneIcon,
  DocPencilIcon,
  PaperclipIcon,
} from "@navikt/aksel-icons";
import type { Journalposttype } from "~/api/journalføring";
import type { TagFarge } from "~/types/farge";
import styles from "./Dokumentliste.module.css";

interface JournalposttypeMeta {
  tekst: string;
  ikon: React.ElementType;
  stilSuffix: string;
  tagFarge: TagFarge;
}

const journalposttypeMeta: Record<Journalposttype, JournalposttypeMeta> = {
  I: {
    tekst: "Innkommende",
    ikon: InboxDownIcon,
    stilSuffix: "I",
    tagFarge: "info",
  },
  U: {
    tekst: "Utgående",
    ikon: PaperplaneIcon,
    stilSuffix: "U",
    tagFarge: "meta-purple",
  },
  N: {
    tekst: "Notat",
    ikon: DocPencilIcon,
    stilSuffix: "N",
    tagFarge: "neutral",
  },
};

function formaterDato(datoStreng?: string): string {
  if (!datoStreng) return "";
  const dato = new Date(datoStreng);
  return dato.toLocaleDateString("no-NO", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export interface Props {
  dokument: Dokumentinfo;
}

const tittelMedUrlGodkjenteTegn = (tittel?: string) => {
  if (!tittel) {
    return 'uten_tittel';
  }
  const tittelUtenTvilsommeTegn = tittel
      .replaceAll('/', '_')
      .replaceAll('\\', '_')
      .replaceAll('%', 'pst');
  return encodeURIComponent(tittelUtenTvilsommeTegn);
};

export const DokumentListeElement: React.FC<Props> = ({ dokument }) => {
  const meta = journalposttypeMeta[dokument.journalposttype];
  const Ikon = meta.ikon;

  return (
    <div className={styles.dokumentRad}>
      <Tooltip content={meta.tekst} placement="left">
        <div
          className={`${styles.dokumentIkonContainer} ${styles[`dokumentIkonContainer_${meta.stilSuffix}`]}`}
        >
          <Ikon className={styles.dokumentIkon} aria-hidden />
        </div>
      </Tooltip>
      <div className={styles.dokumentInnhold}>
        <Tooltip content={dokument.tittel} placement="top">
          <BodyShort size="small" weight="semibold" truncate>
            <a
              href={`/api/saf/${dokument.journalpostId}/dokument-pdf/${dokument.dokumentinfoId}/${tittelMedUrlGodkjenteTegn(dokument.tittel)}`}
              target="_blank"
              rel="noreferrer"
            >
              {dokument.tittel}
            </a>
          </BodyShort>
        </Tooltip>
        <div className={styles.dokumentMeta}>
          {dokument.dato && (
            <>
              <Detail textColor="subtle">{formaterDato(dokument.dato)}</Detail>
              <span className={styles.metaSeparator}>&bull;</span>
            </>
          )}
          <Tag variant="moderate" size="xsmall" data-color={meta.tagFarge}>
            {meta.tekst}
          </Tag>
        </div>
        {dokument.avsenderMottaker?.navn && (
          <Detail textColor="subtle">
            {dokument.journalposttype === "I" ? "Fra" : "Til"}:{" "}
            {dokument.avsenderMottaker.navn}
          </Detail>
        )}
        {dokument.logiskeVedlegg.length > 0 && (
          <div className={styles.vedleggListe}>
            {dokument.logiskeVedlegg.map((vedlegg) => (
              <div key={vedlegg.logiskVedleggId} className={styles.vedleggRad}>
                <PaperclipIcon className={styles.vedleggIkon} aria-hidden />
                <Detail textColor="subtle">{vedlegg.tittel}</Detail>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
