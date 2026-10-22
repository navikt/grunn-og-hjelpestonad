package no.nav.grunn.og.hjelpestonad.infotrygd.infrastruktur.exception

import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.ManglerTilgangTilPersonFeil
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.TilgangsmaskinException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(ApiFeil::class)
    fun handleApiFeil(feil: ApiFeil): ResponseEntity<FeilResponse> {
        logger.warn("En håndtert feil har oppstått (httpStatus={})", feil.httpStatus.value())
        secureLogger.warn("En håndtert feil har oppstått: ${feil.feilmelding}", feil)
        return ResponseEntity
            .status(feil.httpStatus)
            .body(FeilResponse(feil.feilmelding, feil.httpStatus.value()))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<FeilResponse> {
        logger.warn("Ugyldig argument")
        secureLogger.warn("Ugyldig argument: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(e.message ?: "Ugyldig forespørsel", HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(ManglerTilgangTilPersonFeil::class)
    fun handleManglerTilgang(e: ManglerTilgangTilPersonFeil): ResponseEntity<ManglerTilgangResponse> {
        logger.warn("Mangler tilgang til person")
        secureLogger.warn("Mangler tilgang til person: ${e.melding}", e)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ManglerTilgangResponse(melding = e.melding))
    }

    @ExceptionHandler(TilgangsmaskinException::class)
    fun handleTilgangsmaskinException(e: TilgangsmaskinException): ResponseEntity<FeilResponse> {
        logger.warn("Kunne ikke sjekke tilgang mot tilgangsmaskinen")
        secureLogger.warn("Kunne ikke sjekke tilgang mot tilgangsmaskinen", e)
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                FeilResponse(
                    melding = "Vi kunne ikke sjekke om du har tilgang. Prøv igjen om litt.",
                    status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                ),
            )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<FeilResponse> {
        // Re-throw sånn at Spring kan håndtere sin egne resource not found exceptions
        // Dette fikser unødvendig støy i loggene fra actuator-/interne endepunktene
        throw e
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(e: Exception): ResponseEntity<FeilResponse> {
        if (e is AccessDeniedException || e is AuthenticationException) {
            logger.warn("Autentisering eller tilgangskontroll avviste forespørselen")
            secureLogger.warn("Autentisering eller tilgangskontroll avviste forespørselen", e)
            throw e
        }
        // Behold Springs håndtering av blant annet ugyldige requests og ukjente ressurser.
        if (e is ErrorResponse || e is HttpMessageNotReadableException || e is TypeMismatchException) {
            logger.warn("Forespørselen kunne ikke behandles")
            secureLogger.warn("Forespørselen kunne ikke behandles", e)
            throw e
        }

        logger.error("En uventet feil oppstod")
        secureLogger.error("En uventet feil oppstod", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(FeilResponse("En uventet feil oppstod", HttpStatus.INTERNAL_SERVER_ERROR.value()))
    }
}

data class FeilResponse(
    val melding: String,
    val status: Int,
)

data class ManglerTilgangResponse(
    val melding: String,
)
