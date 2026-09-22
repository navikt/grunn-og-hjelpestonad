package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRettResponse
import no.nav.grunn.og.hjelpestonad.vilkår.rett.tilResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    @PostMapping("/fullfor")
    fun fullførSteg(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<PeriodeMedRettResponse>> = ResponseEntity.ok(vilkårVurderingStegService.fullførSteg(behandlingId).map { it.tilResponse() })
}
