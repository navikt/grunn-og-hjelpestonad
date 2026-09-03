package no.nav.gjenlevende.bs.sak.infrastruktur.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice(basePackages = ["no.nav.gjenlevende.bs.sak"])
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<FeilResponse> {
        logger.warn("Feil: ${feil.melding}", feil)
        return ResponseEntity
            .status(feil.httpStatus)
            .body(FeilResponse(melding = feil.melding, status = feil.httpStatus.value()))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<FeilResponse> {
        logger.warn("IllegalArgumentException: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = e.message ?: "Ugyldig request", status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(WebClientResponseException.Forbidden::class)
    fun handleWebClientForbiddenException(e: WebClientResponseException.Forbidden): ResponseEntity<ManglerTilgangResponse> {
        logger.warn("Mangler tilgang til tjeneste (403)")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ManglerTilgangResponse(melding = "Mangler tilgang"))
    }

    @ExceptionHandler(WebClientResponseException::class)
    fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<FeilResponse> {
        val feilmelding =
            try {
                e.responseBodyAsString
            } catch (ex: Exception) {
                "Feil fra downstream tjeneste"
            }

        logger.warn("WebClientResponseException: ${e.statusCode} - $feilmelding")
        return ResponseEntity
            .status(e.statusCode)
            .body(FeilResponse(melding = feilmelding, status = e.statusCode.value()))
    }

    @ExceptionHandler(ManglerTilgang::class)
    fun handleManglerTilgang(manglerTilgang: ManglerTilgang): ResponseEntity<ManglerTilgangResponse> {
        logger.warn("En håndtert tilgangsfeil har oppstått")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ManglerTilgangResponse(
                    melding = manglerTilgang.melding,
                ),
            )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<FeilResponse> {
        // Re-throw sånn at Spring kan håndtere sin egne resource not found exceptions
        // Dette fikser unødvendig støy i loggene fra actuator-/interne endepunktene
        throw e
    }
}

data class FeilResponse(
    val melding: String,
    val status: Int,
)

data class ManglerTilgangResponse(
    val melding: String?,
)
