CREATE TABLE journalpost_for_behandling
(
    id            UUID PRIMARY KEY,
    behandling_id UUID   NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,
    journalpost_id TEXT  NOT NULL
);

CREATE INDEX idx_journalpost_for_behandling_behandling_id ON journalpost_for_behandling (behandling_id);
