package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import io.swagger.v3.oas.annotations.Operation
import no.nav.grunn.og.hjelpestonad.infotrygd.sak.PersonidenterRequest
import no.nav.grunn.og.hjelpestonad.infotrygd.sak.SakRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/stonad")
class StønadController(
    private val stønadRepository: StønadRepository,
    private val sakRepository: SakRepository,
) {
    @Operation(summary = "Sjekker om personidentene har vedtak eller saker i Infotrygd")
    @PostMapping("/eksisterer")
    fun eksisterer(
        @RequestBody request: PersonidenterRequest,
    ): InfotrygdFinnesResponse {
        request.valider()
        return InfotrygdFinnesResponse(
            vedtak = stønadRepository.finnVedtakstreff(request.personidenter),
            saker = sakRepository.finnesSaker(request.personidenter),
        )
    }

    @Operation(summary = "Henter personer med løpende grunn- eller hjelpestønad fra neste måned, som kan migreres")
    @GetMapping("/migreringspersoner")
    fun hentPersonerForMigrering(
        @RequestParam antall: Int,
    ): PersonerForMigreringResponse {
        require(antall > 0) { "antall må være større enn 0" }
        return PersonerForMigreringResponse(stønadRepository.finnPersonerForMigrering(antall))
    }
}
