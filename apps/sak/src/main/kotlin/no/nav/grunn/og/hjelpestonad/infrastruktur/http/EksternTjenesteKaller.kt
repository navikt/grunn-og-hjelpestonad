package no.nav.grunn.og.hjelpestonad.infrastruktur.http

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import kotlin.time.TimeSource

private val logger = LoggerFactory.getLogger("no.nav.grunn.og.hjelpestonad.infrastruktur.http.EksternTjenesteKaller")
private val secureLogger = LoggerFactory.getLogger("secureLogger")
private const val RESSURS_SUKSESS = "SUKSESS"

/** Statuskoder i [håndterHttpStatus] er forventede utfall. Andre HTTP-feil og transportfeil gir BAD_GATEWAY. */
internal fun <R> kallEksternTjeneste(
    tjeneste: String,
    formål: String,
    håndterHttpStatus: Map<HttpStatus, (RestClientResponseException) -> R> = emptyMap(),
    blokk: () -> R,
): R {
    logger.info("Kaller {}: {}", tjeneste, formål)
    val start = TimeSource.Monotonic.markNow()
    val tidMs = { start.elapsedNow().inWholeMilliseconds }

    val resultat =
        try {
            blokk()
        } catch (e: RestClientResponseException) {
            val status = e.statusCode.value()
            val håndter = HttpStatus.resolve(status)?.let { håndterHttpStatus[it] }
            if (håndter != null) {
                logger.info("{} til {}: forventet utfall, status={}, tid={} ms", formål, tjeneste, status, tidMs())
                return håndter(e)
            }
            logger.error("{} til {} feilet, status={}, tid={} ms", formål, tjeneste, status, tidMs())
            secureLogger.error("{} feilet, status={}, body={}", formål, status, e.responseBodyAsString, e)
            throw Feil("$formål feilet med status $status", HttpStatus.BAD_GATEWAY, e)
        } catch (e: RestClientException) {
            logger.error("{} til {} feilet, type={}, tid={} ms", formål, tjeneste, e.javaClass.simpleName, tidMs())
            secureLogger.error("{} feilet", formål, e)
            throw Feil("$formål feilet", HttpStatus.BAD_GATEWAY, e)
        } catch (e: Exception) {
            logger.error("{} til {} feilet, type={}, tid={} ms", formål, tjeneste, e.javaClass.simpleName, tidMs())
            secureLogger.error("{} feilet", formål, e)
            throw e
        }

    logger.info("{} til {} vellykket, tid={} ms", formål, tjeneste, tidMs())
    return resultat
}

internal fun <T : Any, R> kallEksternTjenesteMedRessurs(
    tjeneste: String,
    formål: String,
    håndterHttpStatus: Map<HttpStatus, (RestClientResponseException) -> R> = emptyMap(),
    hentRessurs: () -> Ressurs<T>?,
    vedSuksess: (VellykketSvar<T>) -> R,
): R =
    kallEksternTjeneste(tjeneste = tjeneste, formål = formål, håndterHttpStatus = håndterHttpStatus) {
        val ressurs = hentRessurs()
        val data = ressurs?.data
        if (ressurs == null || ressurs.status != RESSURS_SUKSESS || data == null) {
            throw Feil("$formål feilet med status ${ressurs?.status ?: "tomt svar"}", HttpStatus.BAD_GATEWAY)
        }
        vedSuksess(VellykketSvar(data = data, melding = ressurs.melding))
    }

internal data class VellykketSvar<T>(
    val data: T,
    val melding: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ressurs<T>(
    val data: T?,
    val status: String,
    val melding: String? = null,
    val frontendFeilmelding: String? = null,
)
