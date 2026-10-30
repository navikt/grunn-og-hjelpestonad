package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import io.swagger.v3.oas.annotations.Operation
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.Oppslag
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.TilgangService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/saker")
class SakController(
    private val sakRepository: SakRepository,
    private val tilgangService: TilgangService,
) {
    @Operation(summary = "Henter saker fra Infotrygd (SA_SAK_10) for oppgitte personidenter")
    @PostMapping("/finn")
    fun finnSaker(
        @RequestBody request: PersonidenterRequest,
    ): InfotrygdSakerResponse {
        request.valider()
        tilgangService.validerTilgangTilPersoner(request.personidenter, Oppslag.HENTE_SAKER)
        return InfotrygdSakerResponse(sakRepository.finnSaker(request.personidenter))
    }
}
