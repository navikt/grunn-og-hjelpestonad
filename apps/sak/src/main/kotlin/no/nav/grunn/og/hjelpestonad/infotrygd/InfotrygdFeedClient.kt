package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate

@Component
class InfotrygdFeedClient(
    private val texasClient: TexasClient,
    @Value("\${integrasjoner.infotrygd-feed.url}") infotrygdFeedUrl: String,
    @Value("\${integrasjoner.infotrygd-feed.scope}") private val infotrygdFeedScope: String,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().baseUrl(infotrygdFeedUrl).build()

    fun sendStartBehandling(personIdent: String) {
        send(
            "/startbehandlingsmelding",
            GrunnstønadStartBehandlingRequest(personIdent),
        )
    }

    fun sendVedtak(
        personIdent: String,
        datoStartNyGrunnstønad: LocalDate,
    ) {
        send(
            "/vedtaksmelding",
            GrunnstønadVedtakRequest(personIdent, datoStartNyGrunnstønad),
        )
    }

    private fun send(
        endepunkt: String,
        request: Any,
    ) {
        val maskinToken = texasClient.hentMaskinToken(infotrygdFeedScope)

        restClient
            .post()
            .uri("$API_BASE_URL$endepunkt")
            .headers { it.setBearerAuth(maskinToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    companion object {
        private const val API_BASE_URL = "/api/grunnstonad/v1/feed"
    }
}

data class GrunnstønadStartBehandlingRequest(
    val personIdent: String,
)

data class GrunnstønadVedtakRequest(
    val personIdent: String,
    val datoStartNyGrunnstønad: LocalDate,
)
