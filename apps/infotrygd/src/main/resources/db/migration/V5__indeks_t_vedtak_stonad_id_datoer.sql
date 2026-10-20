-- Migreringsuttrekket filtrerer vedtak på datoene per stønad. Med datoene i indeksen slipper oppslaget å lese heapen.
create index idx_t_vedtak_stonad_id_datoer
    on t_vedtak (stonad_id) include (vedtak_id, dato_innv_fom, dato_innv_tom);
