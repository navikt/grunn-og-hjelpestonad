package no.nav.grunn.og.hjelpestonad.texas

import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder

class StubTexasClient(
    private val oboToken: String = "obo-token",
    private val maskinToken: String = "maskin-token",
) : TexasClient(
        tokenExchangeEndpoint = "http://localhost/token/exchange",
        tokenMachineEndpoint = "http://localhost/token",
        restClientBuilder = testRestClientBuilder(),
    ) {
    val requestedOboAudiences = mutableListOf<String>()
    val requestedMaskinAudiences = mutableListOf<String>()

    override fun hentOboToken(targetAudience: String): String {
        requestedOboAudiences += targetAudience
        return oboToken
    }

    override fun hentMaskinToken(targetAudience: String): String {
        requestedMaskinAudiences += targetAudience
        return maskinToken
    }
}
