export type Journalposttype = 'I' | 'U' | 'N';

export type Journalstatus =
    | 'MOTTATT'
    | 'JOURNALFOERT'
    | 'FERDIGSTILT'
    | 'EKSPEDERT'
    | 'UNDER_ARBEID'
    | 'FEILREGISTRERT'
    | 'UTGAAR'
    | 'AVBRUTT'
    | 'UKJENT_BRUKER'
    | 'RESERVERT'
    | 'OPPLASTING_DOKUMENT'
    | 'UKJENT';

export interface IJournalpostAvsenderMottaker {
    erLikBruker: boolean;
    id?: string;
    land?: string;
    navn?: string;
    type?: AvsenderMottakerIdType;
}

export enum AvsenderMottakerIdType {
    FNR = 'FNR',
    HPRNR = 'HPRNR',
    ORGNR = 'ORGNR',
    UKJENT = 'UKJENT',
    UTL_ORG = 'UTL_ORG',
    NULL = 'NULL',
}
