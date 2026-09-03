CREATE TABLE behandling
(
    id            UUID         PRIMARY KEY,
    fagsak_id     UUID         NOT NULL,
    status        VARCHAR      NOT NULL,

    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,

    CONSTRAINT fk_behandling_fagsak FOREIGN KEY (fagsak_id) REFERENCES fagsak (id)
);

CREATE INDEX idx_behandling_fagsak_id ON behandling (fagsak_id);
