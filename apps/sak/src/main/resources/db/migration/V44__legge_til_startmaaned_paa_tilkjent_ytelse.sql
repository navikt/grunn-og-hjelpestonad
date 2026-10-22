ALTER TABLE tilkjent_ytelse
    ADD COLUMN startmaned TEXT;

UPDATE tilkjent_ytelse ty
SET startmaned = COALESCE(
    (
        SELECT TO_CHAR(MIN(aty.fom), 'YYYY-MM')
        FROM andel_tilkjent_ytelse aty
        WHERE aty.tilkjent_ytelse_id = ty.id
    ),
    TO_CHAR(ty.opprettet_tid, 'YYYY-MM')
);

ALTER TABLE tilkjent_ytelse
    ALTER COLUMN startmaned SET NOT NULL,
    ADD CONSTRAINT tilkjent_ytelse_startmaned_format
        CHECK (startmaned ~ '^[0-9]{4}-(0[1-9]|1[0-2])$');
