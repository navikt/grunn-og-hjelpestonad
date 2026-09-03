-- Legge til id kolonne til vedtak og barnetilsynperiode tabeller
-- og endre barnetilsynperiode til å bruke vedtak_id i stedet for behandling_id

-- 1. Legg til id kolonne til vedtak med unique constraint
ALTER TABLE vedtak ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE vedtak ADD CONSTRAINT vedtak_id_unique UNIQUE (id);

-- 2. Legg til id og vedtak_id kolonne til barnetilsynperiode
ALTER TABLE barnetilsynperiode ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE barnetilsynperiode ADD COLUMN vedtak_id UUID;

-- 3. Populer vedtak_id basert på eksisterende behandling_id relasjon
UPDATE barnetilsynperiode b
SET vedtak_id = v.id
FROM vedtak v
WHERE b.behandling_id = v.behandling_id;

-- 4. Gjør vedtak_id NOT NULL og legg til primary key og foreign key
ALTER TABLE barnetilsynperiode ALTER COLUMN vedtak_id SET NOT NULL;
ALTER TABLE barnetilsynperiode ADD PRIMARY KEY (id);
ALTER TABLE barnetilsynperiode ADD CONSTRAINT fk_barnetilsynperiode_vedtak_id FOREIGN KEY (vedtak_id) REFERENCES vedtak (id);

-- 5. Fjern gammel behandling_id kolonne og constraint fra barnetilsynperiode
ALTER TABLE barnetilsynperiode DROP CONSTRAINT fk_barnetilsynperiode_behandling_id;
ALTER TABLE barnetilsynperiode DROP COLUMN behandling_id;
