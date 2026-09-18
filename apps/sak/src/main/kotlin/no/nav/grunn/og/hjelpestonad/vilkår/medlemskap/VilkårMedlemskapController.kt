package no.nav.grunn.og.hjelpestonad.vilkår.medlemskap

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
@RequestMapping("/api/behandling/{behandlingId}/vilkar/medlemskap")
class VilkårMedlemskapController(
    private val vilkårMedlemskapService: VilkårMedlemskapService,
) {
    @GetMapping
    fun hentMedlemskapPerioder(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<VilkårMedlemskapResponse>> = ResponseEntity.ok(vilkårMedlemskapService.hentPerioder(behandlingId).map { it.tilResponse() })

    @PostMapping
    fun lagreMedlemskapPeriode(
        @PathVariable behandlingId: UUID,
        @RequestBody request: VilkårMedlemskapRequest,
    ): ResponseEntity<VilkårMedlemskapResponse> = ResponseEntity.ok(vilkårMedlemskapService.lagrePeriode(behandlingId, request).tilResponse())

    @DeleteMapping("/{vilkårPeriodeId}")
    fun slettMedlemskapPeriode(
        @PathVariable behandlingId: UUID,
        @PathVariable vilkårPeriodeId: UUID,
    ): ResponseEntity<Void> {
        vilkårMedlemskapService.slettPeriode(behandlingId, vilkårPeriodeId)
        return ResponseEntity.noContent().build()
    }
}
