ALTER TABLE vedtak
    ALTER COLUMN opphor_fom TYPE DATE USING TO_DATE(opphor_fom, 'YYYY-MM');

ALTER TABLE vedtak
    RENAME COLUMN opphor_fom TO opphor_fom_maned;

ALTER TABLE grunnstonad_periode
    ALTER COLUMN dato_fra TYPE DATE USING TO_DATE(dato_fra, 'YYYY-MM'),
    ALTER COLUMN dato_til TYPE DATE USING TO_DATE(dato_til, 'YYYY-MM');

ALTER TABLE grunnstonad_periode
    RENAME COLUMN dato_fra TO fra_maned;

ALTER TABLE grunnstonad_periode
    RENAME COLUMN dato_til TO til_maned;
