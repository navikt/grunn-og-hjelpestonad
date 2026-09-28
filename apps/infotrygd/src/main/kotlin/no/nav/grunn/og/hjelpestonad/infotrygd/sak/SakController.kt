package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/saker")
class SakController(
    private val sakRepository: SakRepository,
) {
    @Operation(summary = "Henter saker fra Infotrygd (SA_SAK_10) for oppgitte personidenter")
    @PostMapping("/finn")
    fun finnSaker(
        @RequestBody request: PersonidenterRequest,
    ): InfotrygdSakResponse {
        request.valider()
        return InfotrygdSakResponse(sakRepository.finnSaker(request.personidenter))
    }
}
