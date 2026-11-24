package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

/**
 * Svaret frontend får når valideringen feiler. Bærer høyst én feil per nøkkel.
 */
data class VilkårValideringFeilResponse(
    val melding: String,
    val status: Int,
    val feil: List<Valideringsfeil>,
)
