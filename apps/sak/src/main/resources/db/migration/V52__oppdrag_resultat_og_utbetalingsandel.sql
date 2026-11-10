-- Oppdraget slik det ble sendt til OS, og kvitteringsstatusen. Se ADR-0011.
CREATE TABLE oppdrag_resultat
(
    behandling_id      UUID         PRIMARY KEY REFERENCES behandling (id),
    fagsak_id          UUID         NOT NULL REFERENCES fagsak (id),
    utbetalingsoppdrag JSONB        NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    kvitteringsmelding TEXT,
    -- clock_timestamp og ikke LOCALTIMESTAMP: LOCALTIMESTAMP er starten på transaksjonen, som kan ligge før låsen på
    -- fagsaken ble tatt. Rekkefølgen brukes til å finne forrige oppdrag på fagsaken.
    opprettet_tid      TIMESTAMP(6) NOT NULL DEFAULT clock_timestamp(),
    endret_tid         TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP
);

CREATE INDEX oppdrag_resultat_fagsak_id_idx ON oppdrag_resultat (fagsak_id, opprettet_tid);

-- Alle løpende andeler etter behandlingen, med kjedefeltene fra utbetalingsgeneratoren.
-- Uendrede andeler lagres på nytt for hver behandling, med samme periode_id.
CREATE TABLE utbetalingsandel
(
    id                  UUID        PRIMARY KEY,
    behandling_id       UUID        NOT NULL REFERENCES oppdrag_resultat (behandling_id),
    fom                 DATE        NOT NULL,
    tom                 DATE        NOT NULL,
    belop               INTEGER     NOT NULL,
    ytelsestype         VARCHAR(50) NOT NULL,
    periode_id          BIGINT      NOT NULL,
    forrige_periode_id  BIGINT,
    kilde_behandling_id UUID        NOT NULL REFERENCES behandling (id),
    CONSTRAINT utbetalingsandel_fom_tom_chk CHECK (fom <= tom),
    CONSTRAINT utbetalingsandel_behandling_periode_uq UNIQUE (behandling_id, periode_id)
);
