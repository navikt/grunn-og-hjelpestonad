package no.nav.gjenlevende.bs.sak.fagsak

import no.nav.gjenlevende.bs.sak.fagsak.dto.FagsakDto
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@Tilgangskontroll
@RequestMapping(path = ["/api/fagsak"])
open class FagsakController(
    private val fagsakService: FagsakService,
) {
    @PostMapping
    fun hentEllerOpprettFagsakForPerson(
        @RequestBody request: FagsakRequest,
    ): ResponseEntity<FagsakDto> = ResponseEntity.ok(fagsakService.hentEllerOpprettFagsak(request))
}
