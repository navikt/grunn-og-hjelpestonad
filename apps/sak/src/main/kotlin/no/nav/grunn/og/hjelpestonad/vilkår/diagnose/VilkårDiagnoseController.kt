package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

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
@RequestMapping("/api/behandling/{behandlingId}/vilkar/diagnose")
class VilkårDiagnoseController(
    private val vilkårDiagnoseService: VilkårDiagnoseService,
    private val tilgangService: TilgangService,
) {
    @GetMapping
    fun hentDiagnosePerioder(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<VilkårDiagnoseResponse>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(vilkårDiagnoseService.hentPerioder(behandlingId).map { it.tilResponse() })
    }

    @PostMapping
    fun lagreDiagnosePeriode(
        @PathVariable behandlingId: UUID,
        @RequestBody request: VilkårDiagnoseRequest,
    ): ResponseEntity<VilkårDiagnoseResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(vilkårDiagnoseService.lagrePeriode(behandlingId, request).tilResponse())
    }

    @DeleteMapping("/{vilkårPeriodeId}")
    fun slettDiagnosePeriode(
        @PathVariable behandlingId: UUID,
        @PathVariable vilkårPeriodeId: UUID,
    ): ResponseEntity<Void> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        vilkårDiagnoseService.slettPeriode(behandlingId, vilkårPeriodeId)
        return ResponseEntity.noContent().build()
    }
}
