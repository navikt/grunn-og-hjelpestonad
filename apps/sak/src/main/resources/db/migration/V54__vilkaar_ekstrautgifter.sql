CREATE TABLE vilkar_ekstrautgifter
(
    id              UUID         PRIMARY KEY,
    behandling_id   UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    vurdering       TEXT         NOT NULL,
    sats            TEXT,
    begrunnelse     TEXT         NOT NULL DEFAULT '',
    fra_og_med_dato DATE,
    til_og_med_dato DATE,
    opprettet_av    TEXT         NOT NULL,
    opprettet_tid   TIMESTAMP(3) NOT NULL,
    endret_av       TEXT         NOT NULL,
    endret_tid      TIMESTAMP(3) NOT NULL,
    CONSTRAINT vilkar_ekstrautgifter_datorekkefolge
        CHECK (til_og_med_dato IS NULL OR til_og_med_dato >= fra_og_med_dato),
    CONSTRAINT vilkar_ekstrautgifter_sats_gyldig
        CHECK (sats IS NULL OR sats IN ('SATS_1', 'SATS_2', 'SATS_3', 'SATS_4', 'SATS_5', 'SATS_6')),
    CONSTRAINT vilkar_ekstrautgifter_sats_ved_ja
        CHECK ((vurdering = 'JA' AND sats IS NOT NULL) OR (vurdering <> 'JA' AND sats IS NULL)),
    CONSTRAINT vilkar_ekstrautgifter_fra_og_med_ved_ja
        CHECK (vurdering <> 'JA' OR fra_og_med_dato IS NOT NULL)
);

CREATE INDEX idx_vilkar_ekstrautgifter_behandling_id ON vilkar_ekstrautgifter (behandling_id);

CREATE TABLE vilkar_ekstrautgift_diagnose
(
    vilkar_ekstrautgifter_id UUID NOT NULL REFERENCES vilkar_ekstrautgifter (id) ON DELETE CASCADE,
    hjemmelbokstav           TEXT NOT NULL,
    diagnosekode             TEXT NOT NULL,
    PRIMARY KEY (vilkar_ekstrautgifter_id, hjemmelbokstav, diagnosekode),
    CONSTRAINT vilkar_ekstrautgift_diagnose_hjemmel_gyldig
        CHECK (hjemmelbokstav IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')),
    CONSTRAINT vilkar_ekstrautgift_diagnose_ikke_tom
        CHECK (length(btrim(diagnosekode)) > 0)
);
