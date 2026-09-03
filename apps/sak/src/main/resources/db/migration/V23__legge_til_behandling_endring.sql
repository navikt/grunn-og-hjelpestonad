CREATE TABLE behandling_endring (
    id             UUID PRIMARY KEY,
    behandling_id  UUID         NOT NULL,
    endring_type   VARCHAR(100) NOT NULL,
    utfort_av      VARCHAR(255) NOT NULL,
    utfort_tid     TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    detaljer       TEXT,
    CONSTRAINT fk_behandling_endring_behandling
        FOREIGN KEY (behandling_id) REFERENCES behandling (id)
);

CREATE INDEX idx_behandling_endring_behandling_id ON behandling_endring (behandling_id);
CREATE INDEX idx_behandling_endring_utfort_tid ON behandling_endring (utfort_tid);
