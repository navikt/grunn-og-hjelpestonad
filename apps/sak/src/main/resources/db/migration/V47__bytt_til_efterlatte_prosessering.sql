CREATE TABLE prosessering_task (
    id            BIGSERIAL PRIMARY KEY,
    type          TEXT        NOT NULL,
    status        TEXT        NOT NULL DEFAULT 'KLAR',
    payload       TEXT,
    trigger_tid   TIMESTAMPTZ NOT NULL DEFAULT now(),
    opprettet_tid TIMESTAMPTZ NOT NULL DEFAULT now(),
    plukket_tid   TIMESTAMPTZ,
    antall_feil   INT         NOT NULL DEFAULT 0,
    stoppaarsak   TEXT,
    versjon       BIGINT      NOT NULL DEFAULT 0,
    metadata      JSONB,
    CONSTRAINT uq_prosessering_task_type_payload UNIQUE (type, payload)
);

CREATE INDEX idx_prosessering_task_plukk
    ON prosessering_task (trigger_tid)
    WHERE status = 'KLAR';

CREATE TABLE prosessering_task_hendelse (
    id         BIGSERIAL   PRIMARY KEY,
    task_id    BIGINT      NOT NULL,
    type       TEXT        NOT NULL,
    melding    TEXT        NOT NULL,
    endret_av  TEXT        NOT NULL,
    node       TEXT        NOT NULL,
    tidspunkt  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_prosessering_task_hendelse_task
    ON prosessering_task_hendelse (task_id, tidspunkt);

DROP TABLE task_logg;
DROP TABLE task;
DROP SEQUENCE task_seq;
