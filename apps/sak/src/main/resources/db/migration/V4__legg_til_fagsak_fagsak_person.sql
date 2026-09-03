CREATE TABLE fagsak
(
    id               UUID         PRIMARY KEY,
    fagsak_person_id UUID         NOT NULL,
    stonadstype      VARCHAR      NOT NULL,
    ekstern_id       BIGINT       NOT NULL,
    opprettet_av     VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP    NOT NULL DEFAULT localtimestamp
);

CREATE TABLE fagsak_person
(
    id               UUID         PRIMARY KEY,
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp
);

CREATE TABLE person_ident
(
    ident            VARCHAR PRIMARY KEY,
    fagsak_person_id UUID         NOT NULL REFERENCES fagsak_person (id),
    opprettet_av     VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);

CREATE INDEX ON person_ident (fagsak_person_id);

CREATE TABLE fagsak_ekstern (
                                id BIGSERIAL primary key,
                                fagsak_id UUID references fagsak(id)
);

ALTER SEQUENCE fagsak_ekstern_id_seq RESTART with 200000000;
