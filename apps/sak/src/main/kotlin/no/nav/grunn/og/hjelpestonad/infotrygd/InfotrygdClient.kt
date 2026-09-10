package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.dto.PersonPerioderResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.dto.PersonidentRequest
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
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

    fun hentPerioderForPerson(personident: String): PersonPerioderResponse {
        val oboToken =
            texasClient.hentOboToken(
                targetAudience = grunnOgHjelpestonadInfotrygdAudience,
            )

        return try {
            restClient
                .post()
                .uri("$API_BASE_URL/perioder")
                .headers { it.setBearerAuth(oboToken) }
                .body(PersonidentRequest(personident = personident))
                .retrieve()
                .body(PersonPerioderResponse::class.java)
                ?.also { response ->
                    logger.info(
                        "Hentet perioder for person: {} barnetilsyn, {} skolepenger",
                        response.barnetilsyn.size,
                        response.skolepenger.size,
                    )
                } ?: throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
        } catch (e: HttpClientErrorException.NotFound) {
            logger.info("Person ikke funnet i Infotrygd")
            throw Feil("Person ikke funnet i Infotrygd", HttpStatus.NOT_FOUND)
        } catch (e: Exception) {
            logger.error("Feilet å hente perioder for person", e)
            throw e
        }
    }

    companion object {
        private const val API_BASE_URL = "/api/infotrygd"
    }
}
