package no.nav.gjenlevende.bs.sak.felles.auditlogger

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
