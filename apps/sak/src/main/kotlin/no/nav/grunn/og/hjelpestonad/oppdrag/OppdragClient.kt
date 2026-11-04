package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
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
        logger.info("Simulerer mot oppdrag: saksnummer={}, {} perioder", utbetalingsoppdrag.saksnummer, utbetalingsoppdrag.utbetalingsperiode.size)

        val data = post<DetaljertSimuleringResultat>(SIMULERING_PATH, utbetalingsoppdrag, "Simulering mot oppdrag").dataVedSuksess("Simulering mot oppdrag")

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

        post<String>(IVERKSETT_PATH, utbetalingsoppdrag, "Iverksetting mot oppdrag") { status ->
            if (status.value() == HttpStatus.CONFLICT.value()) {
                logger.warn("Oppdrag er allerede sendt: saksnummer={}", utbetalingsoppdrag.saksnummer)
                Feil("Oppdrag er allerede sendt for denne behandlingen", HttpStatus.CONFLICT)
            } else {
                null
            }
        }.dataVedSuksess("Iverksetting mot oppdrag")

        logger.info("Oppdrag lagt på kø: saksnummer={}", utbetalingsoppdrag.saksnummer)
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatusSvar {
        val ressurs =
            post<String>(STATUS_PATH, oppdragId, "Henting av oppdragsstatus") { status ->
                if (status.value() == HttpStatus.NOT_FOUND.value()) {
                    Feil("Fant ikke oppdrag for behandlingen", HttpStatus.NOT_FOUND)
                } else {
                    null
                }
            }
        val status = ressurs.dataVedSuksess("Henting av oppdragsstatus")
        logger.info("Oppdragsstatus for fagsystem={}, behandlingsId={}: {}", oppdragId.fagsystem, oppdragId.behandlingsId, status)
        return OppdragStatusSvar(status = status, melding = ressurs?.melding?.takeIf { it.isNotBlank() })
    }

    /** [håndterHttpFeil] kan gi en egen [Feil] for kjente statuskoder. Andre feil blir bad gateway. */
    private inline fun <reified T : Any> post(
        path: String,
        body: Any,
        handling: String,
        håndterHttpFeil: (HttpStatusCode) -> Feil? = { null },
    ): Ressurs<T>? {
        val oboToken = texasClient.hentOboToken(targetAudience = scope)
        return try {
            restClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.setBearerAuth(oboToken) }
                .body(body)
                .retrieve()
                .body<Ressurs<T>>()
        } catch (e: HttpStatusCodeException) {
            secureLogger.error("$handling feilet, status={}, body={}", e.statusCode.value(), e.responseBodyAsString, e)
            håndterHttpFeil(e.statusCode)?.let { throw it }
            logger.error("$handling feilet, status={}", e.statusCode.value())
            throw Feil("$handling feilet med status ${e.statusCode.value()}", HttpStatus.BAD_GATEWAY, e)
        } catch (e: Exception) {
            logger.error("$handling feilet, type={}", e.javaClass.simpleName)
            secureLogger.error("$handling feilet", e)
            throw Feil("$handling feilet", HttpStatus.BAD_GATEWAY, e)
        }
    }

    private fun <T : Any> Ressurs<T>?.dataVedSuksess(handling: String): T {
        val data = this?.data
        if (this?.status != SUKSESS || data == null) {
            logger.error("$handling ga status={}", this?.status)
            secureLogger.error("$handling ga status={}, melding={}", this?.status, this?.melding)
            throw Feil("$handling feilet med status ${this?.status ?: "tomt svar"}", HttpStatus.BAD_GATEWAY)
        }
        return data
    }

    companion object {
        private const val SIMULERING_PATH = "/api/simulering/v1"
        private const val IVERKSETT_PATH = "/api/oppdrag"
        private const val STATUS_PATH = "/api/status"
        private const val SUKSESS = "SUKSESS"
    }
}
