CREATE TABLE behandlingsgrunnlag_pdl_oppholdsadresse
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    landkode            TEXT      NOT NULL,
    gyldig_fra_og_med   TIMESTAMP,
    gyldig_til_og_med   TIMESTAMP,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_oppholdsadresse_behandling_id
    ON behandlingsgrunnlag_pdl_oppholdsadresse (behandling_id);

CREATE TABLE behandlingsgrunnlag_pdl_doedsfall
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    doedsdato           DATE,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_doedsfall_behandling_id
    ON behandlingsgrunnlag_pdl_doedsfall (behandling_id);
