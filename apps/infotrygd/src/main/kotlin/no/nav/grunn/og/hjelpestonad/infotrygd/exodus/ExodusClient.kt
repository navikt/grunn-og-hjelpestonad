package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
open class ExodusClient(
    private val properties: ExodusProperties,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().baseUrl(properties.baseUrl).build()

    open fun hentUttrekk(
        tabell: ExodusTabell,
        iterator: String?,
        antallRader: Int,
    ): HentUttrekkResponse =
        try {
            restClient
                .post()
                .uri("/api/hentUttrekk")
                .headers { it.setBearerAuth(texasClient.hentMaskinToken(properties.scope)) }
                .body(HentUttrekkRequest(tabell.tabellnavn, iterator, antallRader.toLong()))
                .retrieve()
                .body<HentUttrekkResponse>()
                ?: error("Tomt svar fra Exodus for tabell ${tabell.tabellnavn}")
        } catch (e: HttpClientErrorException.Conflict) {
            throw NyBaselineException(tabell, e)
        }
}
