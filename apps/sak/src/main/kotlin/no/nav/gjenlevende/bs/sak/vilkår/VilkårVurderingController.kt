package no.nav.gjenlevende.bs.sak.vilkår

import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tilgangskontroll
@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/vilkar")
class VilkårVurderingController(
    private val vilkårVurderingService: VilkårVurderingService,
) {
    @GetMapping("/{behandlingId}")
    fun hentVilkårVurderinger(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<VilkårVurderingDto>> {
        val vurderinger = vilkårVurderingService.hentVilkårVurderinger(behandlingId)
        return ResponseEntity.ok(vurderinger.map { it.tilDto() })
    }

    @PostMapping("/{behandlingId}")
    fun lagreVilkårVurdering(
        @PathVariable behandlingId: UUID,
        @RequestBody request: VilkårVurderingRequest,
    ): ResponseEntity<VilkårVurderingDto> {
        val vurdering =
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId = behandlingId,
                request = request,
            )
        return ResponseEntity.ok(vurdering.tilDto())
    }
}
