package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

/** Alle tabellene som replikeres fra Exodus. */
enum class ExodusTabell {
    EV_DATA_10,
    EV_VALG_05,
    SA_HENDELSE_20,
    SA_SAK_10,
    SA_STATUS_15,
    T_AVSTEMMING,
    T_DELYTELSE,
    T_ENDRING,
    T_GH,
    T_LOPENR_FNR,
    T_STONAD,
    T_VEDTAK,
    ;

    val tabellnavn: String get() = name.lowercase()
}
