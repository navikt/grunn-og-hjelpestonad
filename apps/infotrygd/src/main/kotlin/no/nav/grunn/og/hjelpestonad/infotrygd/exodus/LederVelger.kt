package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body

/**
 * Leader election via Nais sin elector-sidecar (leaderElection: true). Uten ELECTOR_GET_URL,
 * f.eks. lokalt, regnes denne instansen alltid som leder.
 */
@Component
open class LederVelger(
    @Value("\${ELECTOR_GET_URL:}") private val electorUrl: String,
    @Value("\${HOSTNAME:}") private val podnavn: String,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restClient = restClientBuilder.clone().build()

    open fun erLeder(): Boolean {
        if (electorUrl.isBlank()) return true
        return try {
            restClient
                .get()
                .uri(electorUrl)
                .retrieve()
                .body<ElectorRespons>()
                ?.name == podnavn
        } catch (e: RestClientException) {
            logger.warn("Klarte ikke å avgjøre leder via elector. Antar at denne poden ikke er leder.", e)
            false
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ElectorRespons(
        val name: String,
    )
}
