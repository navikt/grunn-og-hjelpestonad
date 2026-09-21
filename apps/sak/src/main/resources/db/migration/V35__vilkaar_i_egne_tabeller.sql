DROP TABLE IF EXISTS vilkar_diagnose;
DROP TABLE IF EXISTS vilkar_vurdering;

CREATE TABLE vilkar_medlemskap
(
    id              UUID         PRIMARY KEY,
    behandling_id   UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    regelverk       TEXT         NOT NULL,
    vurdering       TEXT         NOT NULL,
    begrunnelse     TEXT         NOT NULL DEFAULT '',
    fra_og_med_dato DATE,
    til_og_med_dato DATE,
    opprettet_av    TEXT         NOT NULL,
    opprettet_tid   TIMESTAMP(3) NOT NULL,
    endret_av       TEXT         NOT NULL,
    endret_tid      TIMESTAMP(3) NOT NULL,
    CONSTRAINT vilkar_medlemskap_datorekkefolge
        CHECK (fra_og_med_dato IS NULL OR til_og_med_dato IS NULL OR til_og_med_dato >= fra_og_med_dato)
);

CREATE INDEX idx_vilkar_medlemskap_behandling_id ON vilkar_medlemskap (behandling_id);

CREATE TABLE vilkar_diagnose
(
    id              UUID         PRIMARY KEY,
    behandling_id   UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    diagnose        TEXT         NOT NULL,
    er_yrkesskade   BOOLEAN      NOT NULL DEFAULT FALSE,
    vurdering       TEXT         NOT NULL,
    begrunnelse     TEXT         NOT NULL DEFAULT '',
    fra_og_med_dato DATE,
    til_og_med_dato DATE,
    opprettet_av    TEXT         NOT NULL,
    opprettet_tid   TIMESTAMP(3) NOT NULL,
    endret_av       TEXT         NOT NULL,
    endret_tid      TIMESTAMP(3) NOT NULL,
    CONSTRAINT vilkar_diagnose_ikke_tom
        CHECK (length(btrim(diagnose)) > 0),
    CONSTRAINT vilkar_diagnose_datorekkefolge
        CHECK (fra_og_med_dato IS NULL OR til_og_med_dato IS NULL OR til_og_med_dato >= fra_og_med_dato)
);

CREATE INDEX idx_vilkar_diagnose_behandling_id ON vilkar_diagnose (behandling_id);

CREATE TABLE vilkar_institusjon
(
    id              UUID         PRIMARY KEY,
    behandling_id   UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    oppholdstype    TEXT,
    unntakshjemmel  TEXT,
    vurdering       TEXT         NOT NULL,
    begrunnelse     TEXT         NOT NULL DEFAULT '',
    fra_og_med_dato DATE,
    til_og_med_dato DATE,
    opprettet_av    TEXT         NOT NULL,
    opprettet_tid   TIMESTAMP(3) NOT NULL,
    endret_av       TEXT         NOT NULL,
    endret_tid      TIMESTAMP(3) NOT NULL,

    CONSTRAINT vilkar_institusjon_unntak_krever_opphold
        CHECK (unntakshjemmel IS NULL OR oppholdstype IS NOT NULL),
    CONSTRAINT vilkar_institusjon_datorekkefolge
        CHECK (fra_og_med_dato IS NULL OR til_og_med_dato IS NULL OR til_og_med_dato >= fra_og_med_dato)
);

CREATE INDEX idx_vilkar_institusjon_behandling_id ON vilkar_institusjon (behandling_id);
