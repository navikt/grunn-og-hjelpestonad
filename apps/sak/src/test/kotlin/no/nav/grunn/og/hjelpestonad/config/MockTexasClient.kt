package no.nav.grunn.og.hjelpestonad.config

import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
@Primary
@Profile("local-mock")
class MockTexasClient(
    restClientBuilder: RestClient.Builder,
) : TexasClient(
        tokenExchangeEndpoint = "mock-exchange",
        tokenMachineEndpoint = "mock-machine",
        restClientBuilder = restClientBuilder,
    ) {
    private val logger = LoggerFactory.getLogger(MockTexasClient::class.java)

    override fun hentOboToken(
        targetAudience: String,
    ): String {
        logger.info("MockTexasClient: Returnerer mock OBO token for target: $targetAudience")
        return "mock-obo-token"
    }

    override fun hentMaskinToken(targetAudience: String): String {
        logger.info("MockTexasClient: Returnerer mock maskin token for target: $targetAudience")
        return "mock-maskin-token"
    }
}
