ALTER TABLE vedtak
    ALTER COLUMN opphor_fom TYPE VARCHAR(7) USING TO_CHAR(opphor_fom, 'YYYY-MM');

ALTER TABLE barnetilsynperiode
    ALTER COLUMN dato_fra TYPE VARCHAR(7) USING TO_CHAR(dato_fra, 'YYYY-MM'),
    ALTER COLUMN dato_til TYPE VARCHAR(7) USING TO_CHAR(dato_til, 'YYYY-MM');
