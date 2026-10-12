package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/fagsak"])
class FagsakController(
    private val fagsakService: FagsakService,
) {
    @PostMapping
    @PreAuthorize("hasRole('SAKSBEHANDLER')")
    fun opprettFagsak(
        @RequestBody request: FagsakRequest,
    ): FagsakResponse {
        validerErPersonident(request.personident)
        return fagsakService.hentEllerOpprettFagsak(personident = request.personident, stønadstype = request.stønadstype)
    }

    @GetMapping("/fagsak-person/{fagsakPersonId}/{stonadstype}")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    fun hentFagsak(
        @PathVariable("fagsakPersonId") fagsakPersonId: UUID,
        @PathVariable("stonadstype") stønadstype: StønadType,
    ): FagsakResponse = fagsakService.hentFagsak(fagsakPersonId = fagsakPersonId, stønadstype = stønadstype)
}
