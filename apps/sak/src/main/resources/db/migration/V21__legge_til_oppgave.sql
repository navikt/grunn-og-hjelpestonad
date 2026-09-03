CREATE TABLE oppgave
(
    id              UUID PRIMARY KEY,
    behandling_id   UUID         NOT NULL,
    gsak_oppgave_id BIGINT       NOT NULL,
    type            VARCHAR      NOT NULL,
    opprettet_av    VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid   TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av       VARCHAR      NOT NULL,
    endret_tid      TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    CONSTRAINT fk_oppgave_behandling FOREIGN KEY (behandling_id) REFERENCES behandling (id)
);

CREATE INDEX idx_oppgave_behandling_id ON oppgave (behandling_id);
