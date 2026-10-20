-- Tellingen av GH-saker per personkey og måned i evalueringsoppslaget leser ellers hele ev_valg_05.
create index idx_ev_valg_05_personkey_aar_mnd_seq on ev_valg_05 (ev01_personkey, ev05_aar_mnd_seq);
