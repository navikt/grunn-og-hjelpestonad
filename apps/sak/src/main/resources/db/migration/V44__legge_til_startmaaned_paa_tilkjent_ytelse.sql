ALTER TABLE tilkjent_ytelse
    ADD COLUMN startmaned DATE;

UPDATE tilkjent_ytelse ty
SET startmaned = COALESCE(
    (
        SELECT date_trunc('month', MIN(aty.fom))::date
        FROM andel_tilkjent_ytelse aty
        WHERE aty.tilkjent_ytelse_id = ty.id
    ),
    date_trunc('month', ty.opprettet_tid)::date
);

ALTER TABLE tilkjent_ytelse
    ALTER COLUMN startmaned SET NOT NULL;
