-- Diagnoseoppslaget slår opp ev_valg_05 på f_nr og henter feltene i ev_data_10 på personkey og måned.
-- Uten disse indeksene leser begge spørringene hele tabellene.
create index idx_ev_valg_05_f_nr on ev_valg_05 (f_nr);
create index idx_ev_data_10_personkey_aar_mnd_seq on ev_data_10 (ev01_personkey, ev05_aar_mnd_seq);
