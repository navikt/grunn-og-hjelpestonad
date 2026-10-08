ALTER TABLE barnetilsynperiode RENAME TO grunnstonad_periode;

ALTER TABLE grunnstonad_periode
    RENAME CONSTRAINT barnetilsynperiode_pkey TO grunnstonad_periode_pkey;

ALTER TABLE grunnstonad_periode
    RENAME CONSTRAINT fk_barnetilsynperiode_vedtak_id TO fk_grunnstonad_periode_vedtak_id;
