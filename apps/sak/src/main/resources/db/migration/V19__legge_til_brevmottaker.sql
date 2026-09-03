CREATE TABLE brevmottaker
(
    id                    UUID PRIMARY KEY,
    behandling_id         UUID NOT NULL REFERENCES behandling (id) ON DELETE CASCADE,

    person_rolle          TEXT NOT NULL,
    mottaker_type         TEXT NOT NULL,

    personident           TEXT NULL,
    orgnr                 TEXT NULL,

    navn_hos_organisasjon TEXT NULL,

    CONSTRAINT chk_mottaker_type_fields CHECK (
        (mottaker_type = 'PERSON' AND personident IS NOT NULL AND orgnr IS NULL)
            OR
        (mottaker_type = 'ORGANISASJON' AND orgnr IS NOT NULL AND personident IS NULL)
        )
);

CREATE INDEX idx_brevmottaker_behandling_id ON brevmottaker (behandling_id);
CREATE INDEX idx_brevmottaker_rolle ON brevmottaker (person_rolle);
CREATE INDEX idx_brevmottaker_orgnr ON brevmottaker (orgnr) WHERE orgnr IS NOT NULL;
CREATE INDEX idx_brevmottaker_person_ident ON brevmottaker (personident) WHERE personident IS NOT NULL;
