CREATE TABLE trygdetid
(
    id              UUID         PRIMARY KEY,
    behandling_id   UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    teller          INT          NOT NULL,
    kilde           TEXT         NOT NULL,
    begrunnelse     TEXT         NOT NULL DEFAULT '',
    fra_og_med_dato DATE,
    til_og_med_dato DATE,
    opprettet_av    TEXT         NOT NULL,
    opprettet_tid   TIMESTAMP    NOT NULL,
    endret_av       TEXT         NOT NULL,
    endret_tid      TIMESTAMP    NOT NULL,
    CONSTRAINT trygdetid_teller_omraade
        CHECK (teller BETWEEN 0 AND 40),
    CONSTRAINT trygdetid_kilde_gyldig
        CHECK (kilde IN ('ALDERSPENSJON', 'UFØRETRYGD', 'INGEN', 'MIGRERING')),
    CONSTRAINT trygdetid_ingen_gir_40
        CHECK (kilde <> 'INGEN' OR teller = 40),
    CONSTRAINT trygdetid_datorekkefolge
        CHECK (fra_og_med_dato IS NULL OR til_og_med_dato IS NULL OR til_og_med_dato >= fra_og_med_dato)
);

CREATE INDEX idx_trygdetid_behandling_id ON trygdetid (behandling_id);
