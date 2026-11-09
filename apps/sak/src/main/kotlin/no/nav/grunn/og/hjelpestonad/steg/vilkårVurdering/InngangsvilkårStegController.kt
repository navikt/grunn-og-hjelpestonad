package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class PeriodeMedOppfylteInngangsvilkårResponse(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
)

/** Inneholder aldri diagnose, jf. GDPR artikkel 9. */
data class InngangsvilkårResponse(
    val perioder: List<PeriodeMedOppfylteInngangsvilkårResponse>,
    val nesteSteg: NesteStegEtterInngangsvilkår,
)

private fun PeriodeMedOppfylteInngangsvilkår.tilResponse() = PeriodeMedOppfylteInngangsvilkårResponse(fraOgMedDato, tilOgMedDato)

private fun InngangsvilkårResultat.tilResponse() =
    InngangsvilkårResponse(
        perioder = perioder.map { it.tilResponse() },
        nesteSteg = nesteSteg,
    )

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/behandling/{behandlingId}/inngangsvilkar")
class InngangsvilkårStegController(
    private val inngangsvilkårStegService: InngangsvilkårStegService,
    private val tilgangService: TilgangService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping("/fullfor")
    fun fullførInngangsvilkår(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<InngangsvilkårResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(inngangsvilkårStegService.fullførSteg(behandlingId).tilResponse())
    }

    @GetMapping("/perioder")
    fun hentPerioderMedOppfylteInngangsvilkår(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<PeriodeMedOppfylteInngangsvilkårResponse>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(inngangsvilkårStegService.hentPerioder(behandlingId).map { it.tilResponse() })
    }

    /**
     * Lokal exception handler slik at springdoc dokumenterer 400-responsen på dette
     * endepunktet alene. Legges den i `ApiExceptionHandler` blir den generisk og dyttes
     * inn på alle endepunkter i APIet.
     */
    @ApiResponse(
        responseCode = "400",
        description = "Vurderingen av inngangsvilkår er ikke komplett. Inneholder én feil per vilkår som mangler.",
        content = [Content(schema = Schema(implementation = VilkårValideringFeilResponse::class))],
    )
    @ExceptionHandler(VilkårValideringFeil::class)
    fun handleVilkårValideringFeil(e: VilkårValideringFeil): ResponseEntity<VilkårValideringFeilResponse> {
        logger.warn("Vilkårsvurderingen er ikke komplett: {} feil", e.feil.size)
        secureLogger.warn("Vilkårsvurderingen er ikke komplett: {}", e.feil, e)
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
