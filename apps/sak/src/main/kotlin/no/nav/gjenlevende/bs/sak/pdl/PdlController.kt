package no.nav.gjenlevende.bs.sak.pdl

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pdl")
class PdlController(
    private val pdlService: PdlService,
) {
    @PostMapping("/person")
    fun hentPerson(
        @RequestBody request: HentPersonRequest,
    ): ResponseEntity<Person> {
        val person =
            pdlService.hentPersonMedFagsakPersonId(request.fagsakPersonId)
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(person)
    }
}
