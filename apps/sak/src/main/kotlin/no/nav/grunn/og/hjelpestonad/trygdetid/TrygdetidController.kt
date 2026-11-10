package no.nav.grunn.og.hjelpestonad.trygdetid

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
@RequestMapping("/api/behandling/{behandlingId}/trygdetid")
class TrygdetidController(
    private val trygdetidService: TrygdetidService,
    private val tilgangService: TilgangService,
) {
    @GetMapping
    fun hentTrygdetidPerioder(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<TrygdetidResponse>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(trygdetidService.hentPerioder(behandlingId).map { it.tilResponse() })
    }

    @PostMapping
    fun lagreTrygdetidPeriode(
        @PathVariable behandlingId: UUID,
        @RequestBody request: TrygdetidRequest,
    ): ResponseEntity<TrygdetidResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(trygdetidService.lagrePeriode(behandlingId, request).tilResponse())
    }

    @DeleteMapping("/{trygdetidId}")
    fun slettTrygdetidPeriode(
        @PathVariable behandlingId: UUID,
        @PathVariable trygdetidId: UUID,
    ): ResponseEntity<Void> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        trygdetidService.slettPeriode(behandlingId, trygdetidId)
        return ResponseEntity.noContent().build()
    }
}
