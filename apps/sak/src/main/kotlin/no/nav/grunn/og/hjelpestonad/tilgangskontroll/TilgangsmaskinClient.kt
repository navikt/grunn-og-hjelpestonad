package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilgangsmaskinClient(
    @Value("\${TILGANGSMASKIN_URL}") private val tilgangsmaskinUrl: URI,
    @Value("\${TILGANGSMASKIN_SCOPE}") private val tilgangsmaskinScope: String,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(TilgangsmaskinClient::class.java)
    private val restClient = restClientBuilder.clone().baseUrl(tilgangsmaskinUrl.toString()).build()

    fun sjekkAnsatt(navIdent: String): AnsattInfoResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(tilgangsmaskinUrl)
                .pathSegment("dev", "ansatt", navIdent)
                .build()
                .toUri()

        return try {
            restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body<AnsattInfoResponse>()
                ?: throw TilgangsmaskinException("Ingen respons fra tilgangsmaskinen")
        } catch (e: TilgangsmaskinException) {
            throw e
        } catch (e: Exception) {
            logger.error("Feil ved henting av ansattinfo fra tilgangsmaskinen", e)
            throw TilgangsmaskinException("Feil ved henting av ansattinfo", e)
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

        return try {
            restClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.setBearerAuth(oboToken) }
                .body(personidenter.toSet())
                .retrieve()
                .body<BulkTilgangsResponse>()
                ?: throw TilgangsmaskinException("Ingen respons fra tilgangsmaskinen (bulk)")
        } catch (e: TilgangsmaskinException) {
            throw e
        } catch (e: Exception) {
            logger.error("Feil ved bulk-sjekk mot tilgangsmaskinen", e)
            throw TilgangsmaskinException("Feil ved bulk-tilgangssjekk", e)
        }
    }
}

class TilgangsmaskinException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
