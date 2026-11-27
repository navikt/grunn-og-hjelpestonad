package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import io.swagger.v3.oas.annotations.Operation
import no.nav.grunn.og.hjelpestonad.infotrygd.sak.PersonidenterRequest
import no.nav.grunn.og.hjelpestonad.infotrygd.sak.SakRepository
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.Oppslag
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.TilgangService
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
    private val tilgangService: TilgangService,
) {
    @Operation(summary = "Sjekker om personidentene har vedtak eller saker i Infotrygd")
    @PostMapping("/eksisterer")
    fun eksisterer(
        @RequestBody request: PersonidenterRequest,
    ): InfotrygdFinnesResponse {
        request.valider()
        tilgangService.validerTilgangTilPersoner(request.personidenter, Oppslag.SJEKKE_OM_FINNES_I_INFOTRYGD)
        return InfotrygdFinnesResponse(
            vedtak = stønadRepository.finnVedtakstreff(request.personidenter),
            saker = sakRepository.finnesSaker(request.personidenter),
        )
    }

    @Operation(
        summary = "Henter personer med løpende grunn- eller hjelpestønad fra neste måned, som kan migreres",
        description =
            "Med brukertoken fjernes personer saksbehandleren ikke har tilgang til, " +
                "så svaret kan inneholde færre enn antall personer.",
    )
    @GetMapping("/migreringspersoner")
    fun hentPersonerForMigrering(
        @RequestParam antall: Int,
    ): PersonerForMigreringResponse {
        require(antall > 0) { "antall må være større enn 0" }
        require(antall <= TilgangService.MAKS_ANTALL_PERSONER) { "antall kan ikke være større enn ${TilgangService.MAKS_ANTALL_PERSONER}" }
        val personer = stønadRepository.finnPersonerForMigrering(antall)
        return PersonerForMigreringResponse(tilgangService.filtrerPersonerMedTilgang(personer))
    }

    @Operation(
        summary = "Henter personer med løpende grunnstønad som bare har ett førstegangsvedtak, med gitt sats og hjemmelbokstav",
        description =
            "Stønaden har ett vedtak med endringskode F når annullerte og uavklarte vedtak er trukket fra. " +
                "Personer med løpende hjelpestønad er ikke med. Hjemmelbokstav er A til H. " +
                "Med brukertoken fjernes personer saksbehandleren ikke har tilgang til, " +
                "så svaret kan inneholde færre enn antall personer.",
    )
    @GetMapping("/migreringspersoner/forstegangsvedtak")
    fun hentPersonerMedFørstegangsvedtakForMigrering(
        @RequestParam antall: Int,
        @RequestParam sats: Int,
        @RequestParam hjemmelbokstav: String,
    ): PersonerForMigreringResponse {
        require(antall > 0) { "antall må være større enn 0" }
        require(antall <= TilgangService.MAKS_ANTALL_PERSONER) { "antall kan ikke være større enn ${TilgangService.MAKS_ANTALL_PERSONER}" }
        require(sats in GYLDIGE_SATSER) { "sats må være fra 1 til 6" }
        val hjemmelbokstavMedStorBokstav = hjemmelbokstav.trim().uppercase()
        require(hjemmelbokstavMedStorBokstav in GYLDIGE_HJEMMELBOKSTAVER) { "hjemmelbokstav må være en av A til H" }
        val personer =
            stønadRepository.finnPersonerMedFørstegangsvedtakForMigrering(
                antall = antall,
                sats = sats,
                hjemmelbokstav = hjemmelbokstavMedStorBokstav,
            )
        return PersonerForMigreringResponse(tilgangService.filtrerPersonerMedTilgang(personer))
    }

    private companion object {
        val GYLDIGE_SATSER = 1..6
        val GYLDIGE_HJEMMELBOKSTAVER = ('A'..'H').map { it.toString() }.toSet()
    }
}
