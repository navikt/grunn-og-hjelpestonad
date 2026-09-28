package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

enum class Stønadstype(
    /** KODE_RUTINE i T_STONAD og T_VEDTAK. */
    val kodeRutine: String,
) {
    GRUNNSTØNAD("GS"),
    HJELPESTØNAD("HS"),
    ;

    companion object {
        fun fraKodeRutine(kodeRutine: String): Stønadstype = entries.find { it.kodeRutine == kodeRutine } ?: error("Ukjent kode_rutine: $kodeRutine")
    }
}
