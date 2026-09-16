package no.nav.grunn.og.hjelpestonad.vilkår

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

/**
 * Eget endepunkt for diagnose, atskilt fra [VilkårVurderingController]. Diagnose er
 * helseopplysning etter GDPR artikkel 9, og et eget endepunkt gir et eget punkt for
 * tilgangsstyring og auditspor, jf. ADR-0003.
 */
@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/vilkar/{behandlingId}/{vurderingId}/diagnoser")
class VilkårDiagnoseController(
    private val vilkårDiagnoseService: VilkårDiagnoseService,
) {
    @GetMapping
    fun hentDiagnoser(
        @PathVariable behandlingId: UUID,
        @PathVariable vurderingId: UUID,
    ): ResponseEntity<List<DiagnoseResponse>> {
        val diagnoser = vilkårDiagnoseService.hentDiagnoser(behandlingId, vurderingId)
        return ResponseEntity.ok(diagnoser.map { it.tilResponse() })
    }

    @PostMapping
    fun lagreDiagnose(
        @PathVariable behandlingId: UUID,
        @PathVariable vurderingId: UUID,
        @RequestBody request: DiagnoseRequest,
    ): ResponseEntity<DiagnoseResponse> {
        val diagnose =
            vilkårDiagnoseService.lagreDiagnose(
                behandlingId = behandlingId,
                vurderingId = vurderingId,
                request = request,
            )
        return ResponseEntity.ok(diagnose.tilResponse())
    }

    @DeleteMapping("/{diagnoseId}")
    fun slettDiagnose(
        @PathVariable behandlingId: UUID,
        @PathVariable vurderingId: UUID,
        @PathVariable diagnoseId: UUID,
    ): ResponseEntity<Void> {
        vilkårDiagnoseService.slettDiagnose(
            behandlingId = behandlingId,
            vurderingId = vurderingId,
            diagnoseId = diagnoseId,
        )
        return ResponseEntity.noContent().build()
    }
}
