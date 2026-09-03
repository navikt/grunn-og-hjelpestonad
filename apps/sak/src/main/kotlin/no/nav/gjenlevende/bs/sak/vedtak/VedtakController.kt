package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
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
@Tilgangskontroll
@RequestMapping("/api/vedtak")
class VedtakController(
    private val vedtakService: VedtakService,
    private val gjeldendeVedtakService: GjeldendeVedtakService,
) {
    @GetMapping("/{behandlingId}/hent-vedtak")
    fun hentVedtak(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<VedtakDto> {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        return if (vedtak == null) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.ok(vedtak.tilDto())
        }
    }

    @PostMapping("/{behandlingId}/lagre-vedtak")
    fun lagreVedtak(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtakDto: VedtakDto,
    ): ResponseEntity<Map<String, String>> {
        vedtakService.validerKanLagreVedtak(vedtakDto)
        vedtakService.slettVedtakHvisFinnes(behandlingId)
        vedtakService.lagreVedtak(vedtakDto = vedtakDto, behandlingId = behandlingId)
        return ResponseEntity.ok(mapOf("status" to "OK"))
    }

    @PostMapping("/{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: UUID,
        @RequestBody barnetilsynBeregningRequest: BarnetilsynBeregningRequest,
    ): ResponseEntity<List<BeløpsperioderDto>> {
        vedtakService.validerKanBeregne(barnetilsynBeregningRequest)
        return ResponseEntity.ok(vedtakService.lagBeløpsperioder(barnetilsynBeregningRequest))
    }

    @GetMapping("{behandlingId}/historikk/{fra}")
    fun hentHistoriskVedtakForBehanling(
        @PathVariable behandlingId: UUID,
        @PathVariable fra: YearMonth,
    ): ResponseEntity<HistoriskVedtakResponse> = ResponseEntity.ok(gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandlingId, fra))
}
