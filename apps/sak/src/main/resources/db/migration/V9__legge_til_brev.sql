CREATE TABLE brev
(
    behandlings_id UUID PRIMARY KEY NOT NULL,
    brev_json      JSONB            NOT NULL,
    brev_pdf       BYTEA,

    opprettet_av   VARCHAR          NOT NULL DEFAULT 'VL',
    opprettet_tid  TIMESTAMP(3)     NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av      VARCHAR          NOT NULL,
    endret_tid     TIMESTAMP(3)     NOT NULL DEFAULT LOCALTIMESTAMP,

    CONSTRAINT fk_brev_behandling FOREIGN KEY (behandlings_id) REFERENCES behandling (id)
);
