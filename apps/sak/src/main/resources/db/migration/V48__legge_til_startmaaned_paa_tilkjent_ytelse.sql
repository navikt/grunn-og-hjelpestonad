DELETE FROM andel_tilkjent_ytelse;
DELETE FROM tilkjent_ytelse;

ALTER TABLE tilkjent_ytelse
    ADD COLUMN startmaned DATE NOT NULL;
