ALTER TABLE behandling_barn DROP COLUMN IF EXISTS soknad_barn_id;
ALTER TABLE behandling_barn ADD COLUMN IF NOT EXISTS fodsel_dato DATE;
ALTER TABLE behandling_barn ADD COLUMN IF NOT EXISTS hentet_tidspunkt TIMESTAMP(3);
