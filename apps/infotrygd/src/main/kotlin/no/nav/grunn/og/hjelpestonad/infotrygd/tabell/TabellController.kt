package no.nav.grunn.og.hjelpestonad.infotrygd.tabell

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/tabeller")
class TabellController(
    private val tabellRepository: TabellRepository,
) {
    @Operation(summary = "Henter kolonnene i hver tabell som replikeres fra Exodus")
    @GetMapping("/kolonner")
    fun hentKolonnerPerTabell(): List<TabellKolonnerResponse> = tabellRepository.hentKolonnerPerTabell().map { (tabell, kolonner) -> TabellKolonnerResponse(tabell, kolonner) }

    @Operation(summary = "Henter antall rader i hver tabell som replikeres fra Exodus")
    @GetMapping("/antall-rader")
    fun hentAntallRaderPerTabell(): List<TabellAntallRaderResponse> = tabellRepository.hentAntallRaderPerTabell().map { (tabell, antallRader) -> TabellAntallRaderResponse(tabell, antallRader) }
}
