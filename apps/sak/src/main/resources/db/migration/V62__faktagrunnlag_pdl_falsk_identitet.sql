CREATE TABLE faktagrunnlag_pdl_falsk_identitet
(
    id                  UUID      PRIMARY KEY,
    behandling_id       UUID      NOT NULL UNIQUE REFERENCES behandling (id) ON DELETE CASCADE,
    er_falsk            BOOLEAN   NOT NULL,
    historisk           BOOLEAN   NOT NULL,
    master              TEXT      NOT NULL,
    gyldighetstidspunkt TIMESTAMP,
    opphoerstidspunkt   TIMESTAMP
);
