data class SaksbehandlerResponse(
    val navIdent: String,
    val visningNavn: String,
    val fornavn: String,
    val etternavn: String,
    val tIdent: String,
    val epost: String,
    val enhet: EnhetResponse,
)

data class EnhetResponse(
    val enhetnummer: String,
    val navn: String,
)
