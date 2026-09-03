-- Endre tabellnavn
ALTER TABLE person_ident RENAME TO personident;

-- Gi nytt navn til indeksen (PostgreSQL gir default-navn basert på tabellnavn)
ALTER INDEX person_ident_fagsak_person_id_idx RENAME TO personident_fagsak_person_id_idx;
