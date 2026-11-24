package no.nav.grunn.og.hjelpestonad.steg.satsOgTrygdetid

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeilResponse
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
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
@RequestMapping("/api/behandling/{behandlingId}/sats-og-trygdetid")
class SatsOgTrygdetidController(
    private val satsOgTrygdetidService: SatsOgTrygdetidService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/fullfor")
    @ApiResponse(responseCode = "204", description = "Tilkjent ytelse er beregnet og lagret.")
    fun fullførSatsOgTrygdetid(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<Void> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        satsOgTrygdetidService.fullførSteg(behandlingId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(
        responseCode = "400",
        description = "Grunnlaget for tilkjent ytelse er ikke komplett. Inneholder høyst én feil per valideringsnøkkel.",
        content = [Content(schema = Schema(implementation = VilkårValideringFeilResponse::class))],
    )
    @ExceptionHandler(VilkårValideringFeil::class)
    fun håndterValideringsfeil(feil: VilkårValideringFeil): ResponseEntity<VilkårValideringFeilResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                VilkårValideringFeilResponse(
                    melding = feil.message ?: "Valideringen er ikke komplett",
                    status = HttpStatus.BAD_REQUEST.value(),
                    feil = feil.feil,
                ),
            )
}
