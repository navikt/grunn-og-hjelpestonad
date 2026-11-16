-- Nye kolonner i efterlatte-prosessering 1.20261112132903_79278de (schema.sql i prosessering-postgres).
-- Tabellen eies av biblioteket, så typene følger schema.sql, også TIMESTAMPTZ.

ALTER TABLE prosessering_task
    ADD COLUMN siste_feilmelding TEXT,
    ADD COLUMN sist_endret_tid TIMESTAMPTZ NOT NULL DEFAULT now();
