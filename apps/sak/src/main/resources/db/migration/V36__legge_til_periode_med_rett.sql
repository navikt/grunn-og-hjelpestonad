CREATE TABLE periode_med_rett
(
    id              UUID         PRIMARY KEY,
    behandling_id   UUID         NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    fra_og_med_dato DATE,
    til_og_med_dato DATE,
    opprettet_av    TEXT         NOT NULL,
    opprettet_tid   TIMESTAMP(3) NOT NULL,
    endret_av       TEXT         NOT NULL,
    endret_tid      TIMESTAMP(3) NOT NULL,
    CONSTRAINT periode_med_rett_datorekkefolge
        CHECK (fra_og_med_dato IS NULL OR til_og_med_dato IS NULL OR til_og_med_dato >= fra_og_med_dato)
);

CREATE INDEX idx_periode_med_rett_behandling_id ON periode_med_rett (behandling_id);
