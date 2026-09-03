ALTER TABLE brev
    RENAME COLUMN behandlings_id TO behandling_id;

ALTER TABLE brev
    RENAME CONSTRAINT fk_brev_behandling TO fk_brev_behandling_id;
