package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

/**
 * Svaret frontend får når vilkårssteget ikke kan fullføres. Bærer én feil per vilkår som
 * mangler, slik at saksbehandler kan lenkes til riktig vilkår.
 */
data class VilkårValideringFeilResponse(
    val melding: String,
    val status: Int,
    val feil: List<Valideringsfeil>,
)
