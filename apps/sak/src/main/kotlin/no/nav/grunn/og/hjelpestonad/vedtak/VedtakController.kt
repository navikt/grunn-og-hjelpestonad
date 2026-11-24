package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @PostMapping("/{behandlingId}/lagre-vedtak")
    fun lagreVedtak(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtakRequest: VedtakRequest,
    ): ResponseEntity<Map<String, String>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        validerBarnetilsynRequest { BarnetilsynVedtakRegler.validerKanLagreVedtak(vedtakRequest) }
        vedtakService.lagreVedtak(vedtakRequest = vedtakRequest, behandlingId = behandlingId)
        return ResponseEntity.ok(mapOf("status" to "OK"))
    }

    @PostMapping("/{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: UUID,
        @RequestBody barnetilsynBeregningRequest: BarnetilsynBeregningRequest,
    ): ResponseEntity<List<BeløpsperioderResponse>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        validerBarnetilsynRequest { BarnetilsynVedtakRegler.validerKanBeregne(barnetilsynBeregningRequest) }
        return ResponseEntity.ok(BeregningUtils.beregnBarnetilsynperiode(barnetilsynBeregningRequest.barnetilsynBeregning))
    }

    @GetMapping("{behandlingId}/historikk/{fra}")
    fun hentHistoriskVedtakForBehandling(
        @PathVariable behandlingId: UUID,
        @PathVariable fra: YearMonth,
    ): ResponseEntity<HistoriskVedtakResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandlingId, fra))
    }

    private fun validerBarnetilsynRequest(valider: () -> Unit) {
        try {
            valider()
        } catch (ugyldigInput: UgyldigInput) {
            // De gamle barnetilsynsendepunktene returnerer 500 ved valideringsfeil.
            throw Feil(ugyldigInput.melding)
        }
    }
}
