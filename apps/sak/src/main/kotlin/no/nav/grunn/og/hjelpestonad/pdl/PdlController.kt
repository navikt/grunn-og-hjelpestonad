package no.nav.grunn.og.hjelpestonad.pdl

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pdl")
class PdlController(
    private val pdlService: PdlService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/person")
    fun hentPerson(
        @RequestBody request: HentPersonRequest,
    ): ResponseEntity<PersonResponse> {
        tilgangService.validerTilgangTilFagsakPerson(request.fagsakPersonId)
        val person =
            pdlService.hentPersonMedFagsakPersonId(request.fagsakPersonId)
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(person)
    }
}
