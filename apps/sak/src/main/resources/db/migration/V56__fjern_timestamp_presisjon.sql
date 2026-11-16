-- Fjerner TIMESTAMP(3) fra tidsstempler, jf. AGENTS.md.
-- Presisjonen runder av mikrosekunder i LocalDateTime, så verdien som leses tilbake
-- er ikke lik den som ble lagret. Defaults og NOT NULL beholdes. Typen er fortsatt
-- uten tidssone.

ALTER TABLE sak
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP;

ALTER TABLE fagsak
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP;

ALTER TABLE fagsak_person
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE personident
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP;

ALTER TABLE behandling
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE brev
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE vedtak
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP;

ALTER TABLE oppgave
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE behandling_endring
    ALTER COLUMN utfort_tid TYPE TIMESTAMP;

ALTER TABLE tilkjent_ytelse
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE vilkar_medlemskap
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE vilkar_diagnose
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;

ALTER TABLE faktagrunnlag_henting
    ALTER COLUMN hentet_tidspunkt TYPE TIMESTAMP;

ALTER TABLE vilkar_ekstrautgifter
    ALTER COLUMN opprettet_tid TYPE TIMESTAMP,
    ALTER COLUMN endret_tid TYPE TIMESTAMP;
