package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

/**
 * Alle tabellene som replikeres fra Exodus, med primærnøkkelen fra Oracle som brukes til å upserte
 * rader i Postgres. Kolonnenavn er lowercase siden Postgres folder unquoted identifiers slik, og
 * [HentUttrekkResponse.tilRader] gir lowercase kolonnenavn.
 */
enum class ExodusTabell(
    val primærnøkkel: List<String>,
) {
    EV_DATA_10(listOf("id_ev_10")),
    EV_VALG_05(listOf("id_ev_05")),
    SA_HENDELSE_20(listOf("id_hend")),
    SA_SAK_10(listOf("id_sak")),
    SA_STATUS_15(listOf("id_status")),
    T_AVSTEMMING(listOf("kode_rutine", "type_avstemming", "avst_id")),
    T_DELYTELSE(listOf("vedtak_id", "type_delytelse", "tidspunkt_reg")),
    T_ENDRING(listOf("vedtak_id", "kode")),
    T_GH(listOf("vedtak_id", "tidspunkt_reg")),
    T_LOPENR_FNR(listOf("person_lopenr")),
    T_STONAD(listOf("stonad_id")),
    T_VEDTAK(listOf("vedtak_id")),
    ;

    val tabellnavn: String get() = name.lowercase()
}
