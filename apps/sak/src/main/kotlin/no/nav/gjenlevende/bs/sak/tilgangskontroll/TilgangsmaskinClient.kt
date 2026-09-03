package no.nav.gjenlevende.bs.sak.tilgangskontroll

import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class TilgangsmaskinWebClientConfig {
    @Bean
    fun tilgangsmaskinWebClient(
        @Value("\${TILGANGSMASKIN_URL}") tilgangsmaskinUrl: String,
    ): WebClient =
        WebClient
            .builder()
            .baseUrl(tilgangsmaskinUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
}

@Component
class TilgangsmaskinClient(
    @Value("\${TILGANGSMASKIN_URL}") private val tilgangsmaskinUrl: URI,
    @Value("\${TILGANGSMASKIN_SCOPE}") private val tilgangsmaskinScope: String,
    private val texasClient: TexasClient,
    private val tilgangsmaskinWebClient: WebClient,
) {
    private val logger = LoggerFactory.getLogger(TilgangsmaskinClient::class.java)

    fun sjekkAnsatt(navIdent: String): AnsattInfoResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(tilgangsmaskinUrl)
                .pathSegment("dev", "ansatt", navIdent)
                .build()
                .toUri()

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        try {
            return tilgangsmaskinWebClient
                .get()
                .uri(uri)
                .headers { it.addAll(headers) }
                .retrieve()
                .bodyToMono<AnsattInfoResponse>()
                .block() ?: throw TilgangsmaskinException("Ingen respons fra tilgangsmaskinen")
        } catch (e: Exception) {
            logger.error("Feil ved henting av ansattinfo fra tilgangsmaskinen: ${e.message}")
            throw TilgangsmaskinException("Feil ved henting av ansattinfo: ${e.message}", e)
        }
    }

    fun sjekkTilgangBulk(
        personidenter: List<String>,
        regelType: RegelType = RegelType.KJERNE_REGELTYPE,
    ): BulkTilgangsResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(tilgangsmaskinUrl)
                .pathSegment("api", "v1", "bulk", "obo", regelType.name)
                .build()
                .toUri()

        val oboToken =
            texasClient.hentOboToken(
                targetAudience = tilgangsmaskinScope,
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(oboToken)
            }

        try {
            return tilgangsmaskinWebClient
                .post()
                .uri(uri)
                .bodyValue(personidenter.toSet())
                .headers { it.addAll(headers) }
                .retrieve()
                .bodyToMono<BulkTilgangsResponse>()
                .block() ?: throw TilgangsmaskinException("Ingen respons fra tilgangsmaskinen (bulk)")
        } catch (e: Exception) {
            logger.error("Feil ved bulk sjekk av tilgang mot tilgangsmaskinen: ${e.message}")
            throw TilgangsmaskinException("Feil ved bulk-tilgangssjekk: ${e.message}", e)
        }
    }
}

class TilgangsmaskinException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
