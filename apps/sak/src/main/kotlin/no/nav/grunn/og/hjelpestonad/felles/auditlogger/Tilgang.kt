package no.nav.grunn.og.hjelpestonad.felles.auditlogger

data class Tilgang(
    val harTilgang: Boolean,
    val begrunnelse: String? = null,
) {
    fun utledÅrsakstekst(): String =
        when (this.begrunnelse) {
            null -> ""
            else -> "Årsak: ${this.begrunnelse}"
        }
}
