-- Stegfremdrift per behandling (ticket 22). DEFAULT gjør at forrige appversjon fortsatt kan opprette behandlinger ved rollback.
ALTER TABLE behandling ADD COLUMN gjeldende_steg TEXT NOT NULL DEFAULT 'ÅRSAK';
