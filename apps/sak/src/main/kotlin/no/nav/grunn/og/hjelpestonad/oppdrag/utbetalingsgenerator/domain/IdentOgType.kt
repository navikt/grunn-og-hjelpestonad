package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain

data class IdentOgType(
    val ident: String,
    val type: Ytelsestype,
) : Comparable<IdentOgType> {
    override fun compareTo(other: IdentOgType): Int = (type.klassifisering + ident).compareTo((other.type.klassifisering + other.ident))
}
