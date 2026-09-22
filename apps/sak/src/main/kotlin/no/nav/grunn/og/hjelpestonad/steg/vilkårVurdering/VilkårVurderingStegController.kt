package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRettResponse
import no.nav.grunn.og.hjelpestonad.vilkår.rett.tilResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/behandling/{behandlingId}/vilkar")
class VilkårVurderingStegController(
    private val vilkårVurderingStegService: VilkårVurderingStegService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/fullfor")
    fun fullførSteg(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<PeriodeMedRettResponse>> = ResponseEntity.ok(vilkårVurderingStegService.fullførSteg(behandlingId).map { it.tilResponse() })

    /**
     * Lokal exception handler slik at springdoc dokumenterer 400-responsen på dette
     * endepunktet alene. Legges den i `ApiExceptionHandler` blir den generisk og dyttes
     * inn på alle endepunkter i APIet.
     */
    @ApiResponse(
        responseCode = "400",
        description = "Vilkårsvurderingen er ikke komplett. Inneholder én feil per vilkår som mangler.",
        content = [Content(schema = Schema(implementation = VilkårValideringFeilResponse::class))],
    )
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
}
