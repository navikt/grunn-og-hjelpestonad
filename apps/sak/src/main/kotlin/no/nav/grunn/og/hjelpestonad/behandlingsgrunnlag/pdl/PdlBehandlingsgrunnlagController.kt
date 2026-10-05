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
    // henter fra db
    @GetMapping
    fun hentBehandlingsgrunnlag(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<PdlBehandlingsgrunnlagResponse> = ResponseEntity.ok(pdlBehandlingsgrunnlagService.hent(behandlingId)?.tilResponse() ?: PdlBehandlingsgrunnlagResponse.IKKE_HENTET)

    // innhenter fra PDL
    @PostMapping
    fun innhentBehandlingsgrunnlag(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<PdlBehandlingsgrunnlagResponse> = ResponseEntity.ok(pdlBehandlingsgrunnlagService.innhentPåNytt(behandlingId).tilResponse())
}
