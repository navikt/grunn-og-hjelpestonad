package no.nav.grunn.og.hjelpestonad.infrastruktur.exception

import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.Valideringsfeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice(basePackages = ["no.nav.grunn.og.hjelpestonad"])
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

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<FeilResponse> {
        logger.warn("IllegalStateException: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FeilResponse(melding = e.message ?: "Ugyldig request", status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(VilkårValideringFeil::class)
    fun handleVilkårValideringFeil(e: VilkårValideringFeil): ResponseEntity<VilkårValideringFeilResponse> {
        logger.warn("Vilkårsvurderingen er ikke komplett: {} feil", e.feil.size)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                VilkårValideringFeilResponse(
                    melding = e.message ?: "Vilkårsvurderingen er ikke komplett",
                    status = HttpStatus.BAD_REQUEST.value(),
                    feil = e.feil,
                ),
            )
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden::class)
    fun handleRestClientForbiddenException(e: HttpClientErrorException.Forbidden): ResponseEntity<ManglerTilgangResponse> {
        logger.warn("Mangler tilgang til tjeneste (403)")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ManglerTilgangResponse(melding = "Mangler tilgang"))
    }

    @ExceptionHandler(RestClientResponseException::class)
    fun handleRestClientResponseException(e: RestClientResponseException): ResponseEntity<FeilResponse> {
        logger.warn("Downstream-kall feilet med status {}", e.statusCode)
        return ResponseEntity
            .status(e.statusCode)
            .body(FeilResponse(melding = "Feil fra downstream-tjeneste", status = e.statusCode.value()))
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

data class VilkårValideringFeilResponse(
    val melding: String,
    val status: Int,
    val feil: List<Valideringsfeil>,
)
