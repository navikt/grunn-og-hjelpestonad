package no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/behandling/{behandlingId}/faktagrunnlag/pdl")
class PdlFaktagrunnlagController(
    private val pdlFaktagrunnlagService: PdlFaktagrunnlagService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/medlemskap")
    fun hentMedlemskapFaktagrunnlag(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<MedlemskapFaktagrunnlagResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(pdlFaktagrunnlagService.hent(behandlingId).tilMedlemskapFaktagrunnlagResponse())
    }

    @PostMapping
    fun innhentFaktagrunnlagFraPdl(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<MedlemskapFaktagrunnlagResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return ResponseEntity.ok(pdlFaktagrunnlagService.innhentFaktagrunnlagFraPdl(behandlingId).tilMedlemskapFaktagrunnlagResponse())
    }
}
