ALTER TABLE fagsak_person ADD COLUMN endret_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE fagsak_person ADD COLUMN endret_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp;
