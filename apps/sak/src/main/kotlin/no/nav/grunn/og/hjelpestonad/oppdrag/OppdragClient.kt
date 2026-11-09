package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
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
        logger.info("Simulerer mot oppdrag: saksnummer={}, {} perioder", utbetalingsoppdrag.saksnummer, utbetalingsoppdrag.utbetalingsperiode.size)

        val data = post<DetaljertSimuleringResultat>(SIMULERING_PATH, utbetalingsoppdrag, "Simulering mot oppdrag").data

        logger.info(
            "Simulering mot oppdrag vellykket: {} mottakere, {} posteringer",
            data.simuleringMottaker.size,
            data.simuleringMottaker.sumOf { it.simulertPostering.size },
        )
        return data
    }

    /**
     * Sender oppdraget til OS. Et vellykket svar betyr bare at `familie-oppdrag-backend` har lagt oppdraget på køen.
     * Kvitteringen fra OS kommer asynkront, og må leses med [hentStatus].
     */
    fun iverksett(utbetalingsoppdrag: Utbetalingsoppdrag) {
        logger.info("Iverksetter mot oppdrag: saksnummer={}, {} perioder", utbetalingsoppdrag.saksnummer, utbetalingsoppdrag.utbetalingsperiode.size)

        post<String>(
            IVERKSETT_PATH,
            utbetalingsoppdrag,
            "Iverksetting mot oppdrag",
            kjenteFeil = mapOf(HttpStatus.CONFLICT to "Oppdrag er allerede sendt for denne behandlingen"),
        )

        logger.info("Oppdrag lagt på kø: saksnummer={}", utbetalingsoppdrag.saksnummer)
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatusSvar {
        val svar =
            post<String>(
                STATUS_PATH,
                oppdragId,
                "Henting av oppdragsstatus",
                kjenteFeil = mapOf(HttpStatus.NOT_FOUND to "Fant ikke oppdrag for behandlingen"),
            )
        logger.info("Oppdragsstatus for fagsystem={}, behandlingsId={}: {}", oppdragId.fagsystem, oppdragId.behandlingsId, svar.data)
        return OppdragStatusSvar(status = svar.data, melding = svar.melding?.takeIf { it.isNotBlank() })
    }

    /** Kjente HTTP-statuskoder i [kjenteFeil] gir egen [Feil] og warn-logg. Andre HTTP-feil, nettverksfeil og ugyldige svar gir bad gateway. */
    private inline fun <reified T : Any> post(
        path: String,
        body: Any,
        handling: String,
        kjenteFeil: Map<HttpStatus, String> = emptyMap(),
    ): VellykketSvar<T> {
        val oboToken = texasClient.hentOboToken(targetAudience = scope)
        val ressurs =
            try {
                restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers { it.setBearerAuth(oboToken) }
                    .body(body)
                    .retrieve()
                    .body<Ressurs<T>>()
            } catch (e: RestClientResponseException) {
                HttpStatus.resolve(e.statusCode.value())?.let { status ->
                    kjenteFeil[status]?.let { melding ->
                        logger.warn("{} ga status {}", handling, status.value())
                        secureLogger.warn("{} ga status {}, body={}", handling, status.value(), e.responseBodyAsString)
                        throw Feil(melding, status, e)
                    }
                }
                logger.error("{} feilet, status={}", handling, e.statusCode.value())
                secureLogger.error("{} feilet, status={}, body={}", handling, e.statusCode.value(), e.responseBodyAsString, e)
                throw Feil("$handling feilet med status ${e.statusCode.value()}", HttpStatus.BAD_GATEWAY, e)
            } catch (e: RestClientException) {
                logger.error("{} feilet, type={}", handling, e.javaClass.simpleName)
                secureLogger.error("{} feilet", handling, e)
                throw Feil("$handling feilet", HttpStatus.BAD_GATEWAY, e)
            }

        val data = ressurs?.data
        if (ressurs == null || ressurs.status != SUKSESS || data == null) {
            val status = ressurs?.status ?: "tomt svar"
            logger.error("{} ga status={}", handling, status)
            secureLogger.error("{} ga status={}, melding={}", handling, status, ressurs?.melding)
            throw Feil("$handling feilet med status $status", HttpStatus.BAD_GATEWAY)
        }
        return VellykketSvar(data = data, melding = ressurs.melding)
    }

    companion object {
        private const val SIMULERING_PATH = "/api/simulering/v1"
        private const val IVERKSETT_PATH = "/api/oppdrag"
        private const val STATUS_PATH = "/api/status"
        private const val SUKSESS = "SUKSESS"
    }
}

private data class VellykketSvar<T>(
    val data: T,
    val melding: String?,
)
