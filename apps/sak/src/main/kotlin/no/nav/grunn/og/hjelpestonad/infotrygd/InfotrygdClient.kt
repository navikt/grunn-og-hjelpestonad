package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderRequest
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderResponse
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient

@Service
class InfotrygdClient(
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
    @Value("\${integrasjoner.infotrygd.url}")
    infotrygdUrl: String,
    @Value("\${integrasjoner.infotrygd.scope}")
    private val grunnOgHjelpestonadInfotrygdAudience: String,
) {
    private val logger = LoggerFactory.getLogger(InfotrygdClient::class.java)
    private val restClient = restClientBuilder.clone().baseUrl(infotrygdUrl).build()

    /** Stønadene for personen og stønadstypen, som et tre fra t_stonad til vedtak, delytelser, gh og evaluering. */
    fun hentStønader(
        personident: String,
        stønadstype: StonaderRequest.Stønadstype,
    ): StonaderResponse {
        val oboToken =
            texasClient.hentOboToken(
                targetAudience = grunnOgHjelpestonadInfotrygdAudience,
            )

        return try {
            restClient
                .post()
                .uri("$API_BASE_URL/stonader")
                .headers { it.setBearerAuth(oboToken) }
                .body(StonaderRequest(personidenter = setOf(personident), stønadstype = stønadstype))
                .retrieve()
                .body(StonaderResponse::class.java)
                ?.also { response ->
                    logger.info(
                        "Hentet stønader fra Infotrygd: stønadstype={}, {} stønader, {} vedtak",
                        stønadstype,
                        response.stonader.size,
                        response.stonader.sumOf { it.vedtak.size },
                    )
                } ?: throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
        } catch (e: HttpClientErrorException.NotFound) {
            logger.info("Person ikke funnet i Infotrygd")
            throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
        } catch (e: HttpStatusCodeException) {
            logger.error("Feilet å hente stønader fra Infotrygd, status={}", e.statusCode.value())
            throw e
        } catch (e: Feil) {
            throw e
        } catch (e: Exception) {
            logger.error("Feilet å hente stønader fra Infotrygd, type={}", e.javaClass.simpleName)
            throw Feil("Kunne ikke hente stønader fra Infotrygd")
        }
    }

    companion object {
        private const val API_BASE_URL = "/api/infotrygd"
    }
}
