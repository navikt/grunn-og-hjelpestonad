package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.DiagnoseRequest
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.DiagnoserResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PeriodeRequest
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PerioderResponse
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
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val restClient = restClientBuilder.clone().baseUrl(infotrygdUrl).build()

    fun hentPerioderForPerson(personident: String): PerioderResponse {
        val oboToken =
            texasClient.hentOboToken(
                targetAudience = grunnOgHjelpestonadInfotrygdAudience,
            )

        return try {
            restClient
                .post()
                .uri("$API_BASE_URL/perioder")
                .headers { it.setBearerAuth(oboToken) }
                .body(PeriodeRequest(personidenter = setOf(personident), stønadstyper = emptySet()))
                .retrieve()
                .body(PerioderResponse::class.java)
                ?.also { response ->
                    logger.info(
                        "Hentet perioder for person: {} grunnstønad, {} hjelpestønad",
                        response.grunnstønad.size,
                        response.hjelpestønad.size,
                    )
                } ?: throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
        } catch (e: HttpClientErrorException.NotFound) {
            logger.info("Person ikke funnet i Infotrygd")
            throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND, e)
        } catch (e: Exception) {
            logger.error("Feilet å hente perioder for person")
            secureLogger.error("Feilet å hente perioder for person", e)
            throw e
        }
    }

    fun hentDiagnoser(
        personident: String,
        stønadstyper: Set<DiagnoseRequest.Stønadstyper> = emptySet(),
    ): DiagnoserResponse {
        val oboToken =
            texasClient.hentOboToken(
                targetAudience = grunnOgHjelpestonadInfotrygdAudience,
            )

        return try {
            restClient
                .post()
                .uri("$API_BASE_URL/diagnose")
                .headers { it.setBearerAuth(oboToken) }
                .body(DiagnoseRequest(personidenter = setOf(personident), stønadstyper = stønadstyper))
                .retrieve()
                .body(DiagnoserResponse::class.java)
                ?.also { response ->
                    logger.info("Hentet {} diagnoser fra Infotrygd", response.diagnoser.size)
                } ?: throw Feil("Fikk tomt svar fra Infotrygd ved henting av diagnoser", HttpStatus.INTERNAL_SERVER_ERROR)
        } catch (e: HttpStatusCodeException) {
            logger.error("Feilet å hente diagnoser fra Infotrygd, status={}", e.statusCode.value())
            secureLogger.error("Feilet å hente diagnoser fra Infotrygd", e)
            throw e
        } catch (e: Feil) {
            throw e
        } catch (e: Exception) {
            logger.error("Feilet å hente diagnoser fra Infotrygd")
            secureLogger.error("Feilet å hente diagnoser fra Infotrygd", e)
            throw Feil("Kunne ikke hente diagnoser fra Infotrygd", throwable = e)
        }
    }

    companion object {
        private const val API_BASE_URL = "/api/infotrygd"
    }
}
