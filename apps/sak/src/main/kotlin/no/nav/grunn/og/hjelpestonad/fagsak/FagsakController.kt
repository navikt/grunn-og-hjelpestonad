package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping(path = ["/api/fagsak"])
open class FagsakController(
    private val fagsakService: FagsakService,
) {
    @PostMapping
    fun hentEllerOpprettFagsakForPerson(
        @RequestBody request: FagsakRequest,
    ): ResponseEntity<FagsakResponse> = ResponseEntity.ok(fagsakService.hentEllerOpprettFagsak(request))
}
