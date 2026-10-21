package no.nav.grunn.og.hjelpestonad.infrastruktur.exception

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangsmaskinException
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
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice(basePackages = ["no.nav.grunn.og.hjelpestonad"])
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<FeilResponse> {
        logger.warn("En håndtert feil har oppstått (httpStatus={})", feil.httpStatus.value())
        secureLogger.warn("En håndtert feil har oppstått: ${feil.melding}", feil)
        return ResponseEntity
            .status(feil.httpStatus)
            .body(FeilResponse(melding = feil.melding, status = feil.httpStatus.value()))
    }

    @ExceptionHandler(UgyldigInput::class)
    fun handleUgyldigInput(ugyldigInput: UgyldigInput): ResponseEntity<FeilResponse> {
        logger.warn("Ugyldig input")
        secureLogger.warn("Ugyldig input: ${ugyldigInput.melding}", ugyldigInput)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = ugyldigInput.melding, status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<FeilResponse> {
        logger.warn("Ugyldig argument")
        secureLogger.warn("Ugyldig argument: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = e.message ?: "Ugyldig request", status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<FeilResponse> {
        logger.warn("Ugyldig tilstand")
        secureLogger.warn("Ugyldig tilstand: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = e.message ?: "Ugyldig request", status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden::class)
    fun handleRestClientForbiddenException(e: HttpClientErrorException.Forbidden): ResponseEntity<ManglerTilgangResponse> {
        logger.warn("Mangler tilgang til tjeneste (403)")
        secureLogger.warn("Mangler tilgang til tjeneste (403)", e)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ManglerTilgangResponse(melding = "Mangler tilgang"))
    }

    @ExceptionHandler(RestClientResponseException::class)
    fun handleRestClientResponseException(e: RestClientResponseException): ResponseEntity<FeilResponse> {
        logger.warn("Downstream-kall feilet med status {}", e.statusCode.value())
        secureLogger.warn("Downstream-kall feilet", e)
        return ResponseEntity
            .status(e.statusCode)
            .body(FeilResponse(melding = "Feil fra downstream-tjeneste", status = e.statusCode.value()))
    }

    @ExceptionHandler(ManglerTilgang::class)
    fun handleManglerTilgang(e: ManglerTilgang): ResponseEntity<ManglerTilgangResponse> {
        logger.warn("En håndtert tilgangsfeil har oppstått")
        secureLogger.warn("En håndtert tilgangsfeil har oppstått: ${e.melding}", e)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ManglerTilgangResponse(
                    melding = e.melding,
                ),
            )
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
    val melding: String?,
)
