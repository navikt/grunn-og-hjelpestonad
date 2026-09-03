package no.nav.gjenlevende.bs.sak.barn

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/barn")
class BarnController(
    private val barnService: BarnService,
) {
    @PostMapping("/hent")
    fun hentBarn(
        @RequestBody request: HentBarnRequest,
    ): ResponseEntity<List<HentBarnResponse>> {
        val barn = barnService.hentBarn(request)

        return ResponseEntity.ok(barn)
    }
}
