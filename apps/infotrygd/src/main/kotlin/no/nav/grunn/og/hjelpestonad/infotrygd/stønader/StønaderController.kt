package no.nav.grunn.og.hjelpestonad.infotrygd.stønader

import io.swagger.v3.oas.annotations.Operation
import no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll.TilgangService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd/stonader")
class StønaderController(
    private val stønaderService: StønaderService,
    private val tilgangService: TilgangService,
) {
    @Operation(
        summary = "Henter stønadene for én stønadstype, med vedtak, endringer, delytelser, trygdetid og sats og evaluering",
        description =
            "Svaret følger tabellene i Infotrygd (t_stonad → t_vedtak → t_endring, t_delytelse og t_gh) uten å " +
                "tolke dataene. Evaluering er avledet. Inneholder helseopplysninger.",
    )
    @PostMapping
    fun hentStønader(
        @RequestBody request: StonaderRequest,
    ): StonaderResponse {
        request.valider()
        tilgangService.validerTilgangTilPersoner(request.personidenter)
        return stønaderService.hentStønader(request)
    }
}
