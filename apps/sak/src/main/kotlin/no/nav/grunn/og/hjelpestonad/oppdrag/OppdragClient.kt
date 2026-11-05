package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/** Klient mot `familie-oppdrag-backend` (teamfamilie), som sender videre til Oppdragsystemet (OS). */
@Service
class OppdragClient(
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
    @Value("\${integrasjoner.oppdrag.url}")
    url: String,
    @Value("\${integrasjoner.oppdrag.scope}")
    private val scope: String,
) {
    private val logger = LoggerFactory.getLogger(OppdragClient::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val restClient = restClientBuilder.clone().baseUrl(url).build()

    fun simuler(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val oboToken = texasClient.hentOboToken(targetAudience = scope)
        logger.info("Simulerer mot oppdrag: saksnummer={}, {} perioder", utbetalingsoppdrag.saksnummer, utbetalingsoppdrag.utbetalingsperiode.size)

        val ressurs =
            try {
                restClient
                    .post()
                    .uri(SIMULERING_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers { it.setBearerAuth(oboToken) }
                    .body(utbetalingsoppdrag)
                    .retrieve()
                    .body<Ressurs<DetaljertSimuleringResultat>>()
            } catch (e: HttpStatusCodeException) {
                logger.error("Simulering mot oppdrag feilet, status={}", e.statusCode.value())
                secureLogger.error("Simulering mot oppdrag feilet, body={}", e.responseBodyAsString, e)
                throw Feil("Simulering mot oppdrag feilet med status ${e.statusCode.value()}", HttpStatus.BAD_GATEWAY, e)
            } catch (e: Exception) {
                logger.error("Kunne ikke simulere mot oppdrag, type={}", e.javaClass.simpleName)
                secureLogger.error("Kunne ikke simulere mot oppdrag", e)
                throw Feil("Kunne ikke simulere mot oppdrag", HttpStatus.BAD_GATEWAY, e)
            }

        val data = ressurs?.data
        if (ressurs?.status != SUKSESS || data == null) {
            logger.error("Simulering mot oppdrag ga status={}", ressurs?.status)
            secureLogger.error("Simulering mot oppdrag ga status={}, melding={}", ressurs?.status, ressurs?.melding)
            throw Feil("Simulering mot oppdrag feilet med status ${ressurs?.status ?: "tomt svar"}", HttpStatus.BAD_GATEWAY)
        }

        logger.info(
            "Simulering mot oppdrag vellykket: {} mottakere, {} posteringer",
            data.simuleringMottaker.size,
            data.simuleringMottaker.sumOf { it.simulertPostering.size },
        )
        return data
    }

    companion object {
        private const val SIMULERING_PATH = "/api/simulering/v1"
        private const val SUKSESS = "SUKSESS"
    }
}
