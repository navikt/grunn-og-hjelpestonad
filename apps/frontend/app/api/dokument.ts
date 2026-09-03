import type {IJournalpostAvsenderMottaker, Journalposttype, Journalstatus} from "~/api/journalføring";

export interface Dokumentinfo {
    dokumentinfoId: string;
    filnavn?: string;
    tittel: string;
    tema: string;
    journalpostId: string;
    dato?: string;
    journalposttype: Journalposttype;
    journalstatus: Journalstatus;
    harSaksbehandlerTilgang: boolean;
    logiskeVedlegg: LogiskVedlegg[];
    avsenderMottaker?: IJournalpostAvsenderMottaker;
}

export interface LogiskVedlegg {
    tittel: string;
    logiskVedleggId: string;
}