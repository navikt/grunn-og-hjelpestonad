package no.nav.grunn.og.hjelpestonad.vilkår.institusjon

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
@RequestMapping("/api/behandling/{behandlingId}/vilkar/institusjon")
class VilkårInstitusjonController(
    private val vilkårInstitusjonService: VilkårInstitusjonService,
) {
    @GetMapping
    fun hentInstitusjonPerioder(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<VilkårInstitusjonResponse>> = ResponseEntity.ok(vilkårInstitusjonService.hentPerioder(behandlingId).map { it.tilResponse() })

    @PostMapping
    fun lagreInstitusjonPeriode(
        @PathVariable behandlingId: UUID,
        @RequestBody request: VilkårInstitusjonRequest,
    ): ResponseEntity<VilkårInstitusjonResponse> = ResponseEntity.ok(vilkårInstitusjonService.lagrePeriode(behandlingId, request).tilResponse())

    @DeleteMapping("/{vilkårPeriodeId}")
    fun slettInstitusjonPeriode(
        @PathVariable behandlingId: UUID,
        @PathVariable vilkårPeriodeId: UUID,
    ): ResponseEntity<Void> {
        vilkårInstitusjonService.slettPeriode(behandlingId, vilkårPeriodeId)
        return ResponseEntity.noContent().build()
    }
}
