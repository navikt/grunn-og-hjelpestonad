package no.nav.grunn.og.hjelpestonad.steg.satsogtrygdetid

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.UtledningAvTilkjentYtelseFeil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.Valideringsnøkkel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class SatsOgTrygdetidValideringsfeil(
    val nøkkel: String,
    val melding: String,
)

data class SatsOgTrygdetidFeilResponse(
    val melding: String,
    val status: Int = 400,
    val feil: List<SatsOgTrygdetidValideringsfeil>,
)

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/behandling/{behandlingId}/sats-og-trygdetid")
class SatsOgTrygdetidController(
    private val satsOgTrygdetidStegService: SatsOgTrygdetidStegService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/fullfor")
    @ApiResponse(responseCode = "204", description = "Tilkjent ytelse er beregnet og lagret.")
    fun fullførSatsOgTrygdetid(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<Void> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        satsOgTrygdetidStegService.fullførSteg(behandlingId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(
        responseCode = "400",
        description = "Grunnlaget for tilkjent ytelse er ikke komplett.",
        content = [Content(schema = Schema(implementation = SatsOgTrygdetidFeilResponse::class))],
    )
    @ExceptionHandler(UtledningAvTilkjentYtelseFeil::class)
    fun håndterUtledningsfeil(feil: UtledningAvTilkjentYtelseFeil): ResponseEntity<SatsOgTrygdetidFeilResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            SatsOgTrygdetidFeilResponse(
                melding = "Fullfør sats og trygdetid før du går videre.",
                feil = feil.feil.map { SatsOgTrygdetidValideringsfeil(if (it.nøkkel == Valideringsnøkkel.Trygdetid) "trygdetid" else "ekstrautgifter", it.melding) },
            ),
        )

    @ExceptionHandler(VilkårValideringFeil::class)
    fun håndterInngangsvilkårsfeil(feil: VilkårValideringFeil): ResponseEntity<SatsOgTrygdetidFeilResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            SatsOgTrygdetidFeilResponse(
                melding = "Fullfør inngangsvilkårene før du går videre.",
                feil = feil.feil.map { SatsOgTrygdetidValideringsfeil("inngangsvilkår", it.melding) },
            ),
        )
}
