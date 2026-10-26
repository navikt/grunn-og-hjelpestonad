CREATE SEQUENCE behandling_ekstern_id_seq START WITH 1;

${ignoreIfProd} ALTER SEQUENCE behandling_ekstern_id_seq RESTART WITH 200000000;

ALTER TABLE behandling ADD COLUMN ekstern_id BIGINT;

UPDATE behandling SET ekstern_id = nextval('behandling_ekstern_id_seq');

ALTER TABLE behandling ALTER COLUMN ekstern_id SET NOT NULL;

ALTER TABLE behandling ADD CONSTRAINT behandling_ekstern_id_unique UNIQUE (ekstern_id);
