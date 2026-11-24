package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/vedtak")
class VedtakController(
    private val vedtakService: VedtakService,
    private val gjeldendeVedtakService: GjeldendeVedtakService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}/hent-vedtak")
    fun hentVedtak(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<VedtakResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId)

        return if (vedtak == null) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.ok(vedtak.tilResponse())
        }
    }

    @GetMapping("{behandlingId}/historikk/{fra}")
    fun hentHistoriskVedtakForBehandling(
        @PathVariable behandlingId: UUID,
        @PathVariable fra: YearMonth,
    ): ResponseEntity<HistoriskVedtakResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandlingId, fra))
    }
}
