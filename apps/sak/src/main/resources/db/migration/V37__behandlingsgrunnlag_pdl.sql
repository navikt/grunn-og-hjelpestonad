CREATE TABLE behandlingsgrunnlag_henting
(
    id               UUID         PRIMARY KEY,
    behandling_id    UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    kilde            TEXT         NOT NULL,
    hentet_tidspunkt TIMESTAMP(3) NOT NULL,
    CONSTRAINT behandlingsgrunnlag_henting_kilde CHECK (kilde IN ('PDL', 'MEDL')),
    CONSTRAINT behandlingsgrunnlag_henting_unik_kilde UNIQUE (behandling_id, kilde)
);

CREATE TABLE behandlingsgrunnlag_pdl_folkeregisterpersonstatus
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    status              TEXT      NOT NULL,
    forenklet_status    TEXT      NOT NULL,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_folkeregisterpersonstatus_behandling_id
    ON behandlingsgrunnlag_pdl_folkeregisterpersonstatus (behandling_id);

CREATE TABLE behandlingsgrunnlag_pdl_bostedsadresse
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    adressetype         TEXT,
    kommunenummer       TEXT,
    bostedskommune      TEXT,
    landkode            TEXT,
    gyldig_fra_og_med   TIMESTAMP,
    gyldig_til_og_med   TIMESTAMP,
    angitt_flyttedato   DATE,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP,
    CONSTRAINT behandlingsgrunnlag_pdl_bostedsadresse_adressetype
        CHECK (adressetype IN ('VEGADRESSE', 'MATRIKKELADRESSE', 'UTENLANDSK_ADRESSE', 'UKJENT_BOSTED'))
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_bostedsadresse_behandling_id
    ON behandlingsgrunnlag_pdl_bostedsadresse (behandling_id);

CREATE TABLE behandlingsgrunnlag_pdl_statsborgerskap
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    land                TEXT      NOT NULL,
    gyldig_fra_og_med   DATE,
    gyldig_til_og_med   DATE,
    bekreftelsesdato    DATE,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_statsborgerskap_behandling_id
    ON behandlingsgrunnlag_pdl_statsborgerskap (behandling_id);

CREATE TABLE behandlingsgrunnlag_pdl_opphold
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    type                TEXT      NOT NULL,
    opphold_fra         DATE,
    opphold_til         DATE,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_opphold_behandling_id
    ON behandlingsgrunnlag_pdl_opphold (behandling_id);

CREATE TABLE behandlingsgrunnlag_pdl_innflytting_til_norge
(
    id                          UUID      PRIMARY KEY,
    behandling_id               UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    fraflyttingsland            TEXT,
    fraflyttingssted_i_utlandet TEXT,
    historisk                   BOOLEAN   NOT NULL,
    master                      TEXT      NOT NULL,
    gyldighetstidspunkt         TIMESTAMP,
    opphoerstidspunkt           TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_innflytting_til_norge_behandling_id
    ON behandlingsgrunnlag_pdl_innflytting_til_norge (behandling_id);

CREATE TABLE behandlingsgrunnlag_pdl_utflytting_fra_norge
(
    id                          UUID      PRIMARY KEY,
    behandling_id               UUID      NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    tilflyttingsland            TEXT,
    tilflyttingssted_i_utlandet TEXT,
    utflyttingsdato             DATE,
    historisk                   BOOLEAN   NOT NULL,
    master                      TEXT      NOT NULL,
    gyldighetstidspunkt         TIMESTAMP,
    opphoerstidspunkt           TIMESTAMP
);

CREATE INDEX idx_behandlingsgrunnlag_pdl_utflytting_fra_norge_behandling_id
    ON behandlingsgrunnlag_pdl_utflytting_fra_norge (behandling_id);
