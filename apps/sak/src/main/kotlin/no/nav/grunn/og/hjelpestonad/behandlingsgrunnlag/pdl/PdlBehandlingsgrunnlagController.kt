package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

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
@RequestMapping("/api/behandling/{behandlingId}/behandlingsgrunnlag/pdl")
class PdlBehandlingsgrunnlagController(
    private val pdlBehandlingsgrunnlagService: PdlBehandlingsgrunnlagService,
) {
    @GetMapping("/medlemskap")
    fun hentMedlemskapBehandlingsgrunnlag(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<MedlemskapBehandlingsgrunnlagResponse> = ResponseEntity.ok(pdlBehandlingsgrunnlagService.hent(behandlingId).tilMedlemskapBehandlingsgrunnlagResponse())

    @PostMapping
    fun innhentBehandlingsgrunnlagFraPdl(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<MedlemskapBehandlingsgrunnlagResponse> = ResponseEntity.ok(pdlBehandlingsgrunnlagService.innhentBehandlingsgrunnlagFraPdl(behandlingId).tilMedlemskapBehandlingsgrunnlagResponse())
}
