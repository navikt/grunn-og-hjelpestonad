CREATE TABLE vilkar_vurdering (
    id UUID PRIMARY KEY,
    behandling_id UUID NOT NULL REFERENCES behandling(id) ON DELETE CASCADE,
    vilkar_type VARCHAR(50) NOT NULL,
    vurdering VARCHAR(10) NOT NULL,
    begrunnelse TEXT NOT NULL DEFAULT '',
    opprettet_av VARCHAR(255) NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av VARCHAR(255) NOT NULL,
    endret_tid TIMESTAMP(3) NOT NULL
);

CREATE INDEX idx_vilkar_vurdering_behandling_id ON vilkar_vurdering(behandling_id);
