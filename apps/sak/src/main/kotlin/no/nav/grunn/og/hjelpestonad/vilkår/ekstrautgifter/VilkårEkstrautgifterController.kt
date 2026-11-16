package no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/behandling/{behandlingId}/vilkar/ekstrautgifter")
class VilkårEkstrautgifterController(
    private val vilkårEkstrautgifterService: VilkårEkstrautgifterService,
    private val tilgangService: TilgangService,
) {
    @GetMapping
    fun hentEkstrautgifterPerioder(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<VilkårEkstrautgifterResponse>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(vilkårEkstrautgifterService.hentPerioder(behandlingId).map { it.tilResponse() })
    }

    @PostMapping
    fun lagreEkstrautgifterPeriode(
        @PathVariable behandlingId: UUID,
        @RequestBody request: VilkårEkstrautgifterRequest,
    ): ResponseEntity<VilkårEkstrautgifterResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(vilkårEkstrautgifterService.lagrePeriode(behandlingId, request).tilResponse())
    }

    @DeleteMapping("/{vilkårPeriodeId}")
    fun slettEkstrautgifterPeriode(
        @PathVariable behandlingId: UUID,
        @PathVariable vilkårPeriodeId: UUID,
    ): ResponseEntity<Void> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        vilkårEkstrautgifterService.slettPeriode(behandlingId, vilkårPeriodeId)
        return ResponseEntity.noContent().build()
    }
}
