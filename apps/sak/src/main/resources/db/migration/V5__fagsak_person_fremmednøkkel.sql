ALTER TABLE fagsak ADD CONSTRAINT fagsak_fagsak_person_id_fkey FOREIGN KEY (fagsak_person_id) REFERENCES fagsak_person(id);
CREATE INDEX ON fagsak (fagsak_person_id);
ALTER TABLE fagsak ADD CONSTRAINT fagsak_person_unique UNIQUE (fagsak_person_id, stonadstype);

DROP TABLE fagsak_ekstern;
CREATE SEQUENCE fagsak_ekstern_id_seq START WITH 1;
