-- Rette opp datatyper for vedtak og barnetilsynperiode
-- opphor_fom skal være DATE (YearMonth) ikke TIMESTAMP
-- dato_fra og dato_til skal være DATE (YearMonth) ikke TIMESTAMP
-- utgifter skal være NUMERIC ikke INT

ALTER TABLE vedtak
    ALTER COLUMN opphor_fom TYPE DATE USING opphor_fom::DATE,
    ALTER COLUMN opphor_fom DROP DEFAULT;

ALTER TABLE barnetilsynperiode
    ALTER COLUMN dato_fra TYPE DATE USING dato_fra::DATE,
    ALTER COLUMN dato_fra DROP DEFAULT,
    ALTER COLUMN dato_til TYPE DATE USING dato_til::DATE,
    ALTER COLUMN dato_til DROP DEFAULT,
    ALTER COLUMN utgifter TYPE NUMERIC USING utgifter::NUMERIC;
