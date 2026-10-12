package no.nav.grunn.og.hjelpestonad.vilkår.rett

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/behandling/{behandlingId}/perioder-med-rett")
class PeriodeMedRettController(
    private val periodeMedRettService: PeriodeMedRettService,
    private val tilgangService: TilgangService,
) {
    @GetMapping
    fun hentPerioderMedRett(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<PeriodeMedRettResponse>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(periodeMedRettService.hentPerioderMedRett(behandlingId).map { it.tilResponse() })
    }
}
