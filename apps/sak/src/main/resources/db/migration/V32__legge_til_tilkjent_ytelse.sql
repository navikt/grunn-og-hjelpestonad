CREATE TABLE tilkjent_ytelse
(
    id            UUID         PRIMARY KEY,
    behandling_id UUID         NOT NULL UNIQUE REFERENCES behandling (id),
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);

CREATE TABLE andel_tilkjent_ytelse
(
    id                  UUID    PRIMARY KEY,
    tilkjent_ytelse_id  UUID    NOT NULL REFERENCES tilkjent_ytelse (id),
    belop               INT     NOT NULL,
    fom                 DATE    NOT NULL,
    tom                 DATE    NOT NULL,
    kilde_behandling_id UUID    NOT NULL REFERENCES behandling (id)
);
