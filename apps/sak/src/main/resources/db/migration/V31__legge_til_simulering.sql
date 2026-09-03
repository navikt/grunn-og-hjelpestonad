CREATE TABLE simulering
(
    behandling_id UUID        PRIMARY KEY REFERENCES behandling (id),
    status        TEXT        NOT NULL,
    respons       JSONB,
    opprettet_tid TIMESTAMP   NOT NULL DEFAULT NOW()
);
